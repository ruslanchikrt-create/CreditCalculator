package com.mathprogress.app

import kotlin.math.*

data class SolveResult(
    val success:Boolean,
    val type:String,
    val input:String,
    val answer:String,
    val steps:List<String>,
    val numericAnswers:List<Double> = emptyList(),
    val error:String? = null
)

object MathEngine {
    private const val EPS=1e-8

    fun solve(raw:String, forcedType:String="auto"):SolveResult {
        val input=raw.trim()
        if(input.isBlank()) return fail(input,"Введите условие задачи.")
        return try {
            when(forcedType.lowercase()) {
                "linear","линейное" -> polynomial(input,1)
                "quadratic","квадратное" -> polynomial(input,2)
                "cubic","кубическое" -> polynomial(input,3)
                "system","система" -> system(input)
                "fraction","дроби" -> fraction(input)
                "root","корни" -> root(input)
                "exponential","показательное" -> exponential(input)
                "log","логарифмическое" -> logarithm(input)
                else -> auto(input)
            }
        } catch(e:Exception) { fail(input,"Не удалось разобрать запись: ${e.message ?: "проверьте формат"}") }
    }

    private fun auto(input:String):SolveResult {
        val s=n(input).lowercase()
        return when {
            s.contains("\n") || s.contains(";") -> system(input)
            s.contains("sqrt") || s.contains("√") -> root(input)
            s.contains("log") || s.contains("ln") -> logarithm(input)
            Regex("^[0-9.]+\\^.*x.*=").containsMatchIn(s.replace(" ","")) -> exponential(input)
            s.contains("/") -> fraction(input)
            else -> polynomial(input,null)
        }
    }

    private fun polynomial(input:String, expected:Int?):SolveResult {
        val c=parseEquation(input) ?: return fail(input,"Пример: 2x+3=7, x^2-5x+6=0, x^3-6x^2+11x-6=0")
        val d=degree(c)
        if(expected!=null && d!=expected) return fail(input,"Получилось уравнение степени $d.")
        return solveCoefficients(input,c)
    }

    private fun solveCoefficients(input:String,c:DoubleArray, prefix:List<String> = emptyList(), type:String?=null):SolveResult {
        val d=degree(c); val st=prefix.toMutableList(); st += "Приводим к виду: ${polyText(c)} = 0."
        return when(d) {
            0 -> if(abs(c[0])<EPS) SolveResult(true,type?:"Тождество",input,"Подходит любое x",st+"Получили 0 = 0.") else SolveResult(true,type?:"Уравнение",input,"Решений нет",st+"Получили противоречие.")
            1 -> {
                val a=c[1]; val b=c[0]; val x=-b/a
                st += "${f(a)}x ${sign(b)} = 0."
                st += "${f(a)}x = ${f(-b)}."
                st += "x = ${f(x)}."
                SolveResult(true,type?:"Линейное уравнение",input,"x = ${f(x)}",st,listOf(x))
            }
            2 -> {
                val a=c[2];val b=c[1];val cc=c[0];val D=b*b-4*a*cc
                st += "a=${f(a)}, b=${f(b)}, c=${f(cc)}."
                st += "D=b²−4ac=${f(D)}."
                when {
                    D>EPS -> { val q=sqrt(D);val x1=(-b+q)/(2*a);val x2=(-b-q)/(2*a);st+="D>0, два корня.";st+="x₁=${f(x1)}; x₂=${f(x2)}.";SolveResult(true,type?:"Квадратное уравнение",input,"x₁ = ${f(x1)}, x₂ = ${f(x2)}",st,listOf(x1,x2)) }
                    abs(D)<=EPS -> { val x=-b/(2*a);st+="D=0, один корень x=${f(x)}.";SolveResult(true,type?:"Квадратное уравнение",input,"x = ${f(x)}",st,listOf(x)) }
                    else -> { st+="D<0, действительных корней нет.";SolveResult(true,type?:"Квадратное уравнение",input,"Действительных корней нет",st) }
                }
            }
            3 -> cubic(input,c,st,type)
            else -> fail(input,"В этой версии поддерживаются многочлены до третьей степени.")
        }
    }

    private fun cubic(input:String,c:DoubleArray,st:MutableList<String>,type:String?):SolveResult {
        val a=c[3];val b=c[2];val cc=c[1];val d=c[0]
        val A=b/a;val B=cc/a;val C=d/a
        val p=B-A*A/3.0;val q=2*A*A*A/27.0-A*B/3.0+C
        val delta=q*q/4+p*p*p/27
        st += "Делим на ${f(a)} и применяем формулу Кардано."
        st += "p=${f(p)}, q=${f(q)}, Δ=${f(delta)}."
        val roots=mutableListOf<Double>()
        if(delta>EPS){val u=cbrt(-q/2+sqrt(delta));val v=cbrt(-q/2-sqrt(delta));roots+=u+v-A/3;st+="Δ>0: один действительный корень."}
        else if(abs(delta)<=EPS){val u=cbrt(-q/2);roots+=2*u-A/3;roots+=-u-A/3;st+="Δ=0: есть кратный корень."}
        else {val r=2*sqrt(-p/3);val phi=acos((3*q/(2*p)*sqrt(-3/p)).coerceIn(-1.0,1.0));for(k in 0..2) roots+=r*cos((phi+2*PI*k)/3)-A/3;st+="Δ<0: три действительных корня."}
        val u=roots.distinctBy{round(it*1e7).toLong()}.sorted();u.forEachIndexed{i,x->st+="x${i+1}=${f(x)}."}
        val ans=u.mapIndexed{i,x->if(u.size==1)"x = ${f(x)}" else "x${i+1} = ${f(x)}"}.joinToString(", ")
        return SolveResult(true,type?:"Кубическое уравнение",input,ans,st,u)
    }

    private fun system(input:String):SolveResult {
        val lines=n(input).split('\n',';').map{it.trim()}.filter{it.isNotEmpty()}
        if(lines.size !in 2..3) return fail(input,"Введите 2 или 3 уравнения через ; или с новой строки.")
        val vars=if(lines.size==2) listOf('x','y') else listOf('x','y','z')
        val rows=lines.map{parseLinear(it,vars) ?: return fail(input,"Не удалось разобрать: $it")}
        val m=Array(lines.size){r->rows[r].copyOf()};val st=mutableListOf("Записываем систему и применяем метод Гаусса.")
        for(col in m.indices){var p=col;for(r in col until m.size)if(abs(m[r][col])>abs(m[p][col]))p=r;if(abs(m[p][col])<EPS)return fail(input,"Нет единственного решения.");if(p!=col){val t=m[p];m[p]=m[col];m[col]=t;st+="Меняем строки ${p+1} и ${col+1}."};val q=m[col][col];for(j in col..m.size)m[col][j]/=q;for(r in m.indices)if(r!=col){val k=m[r][col];for(j in col..m.size)m[r][j]-=k*m[col][j]}}
        val vals=m.indices.map{m[it][m.size]};val ans=vars.mapIndexed{i,v->"$v = ${f(vals[i])}"}.joinToString(", ");st+="Получаем: $ans.";return SolveResult(true,"Система ${m.size}×${m.size}",input,ans,st,vals)
    }

    private fun fraction(input:String):SolveResult {
        val s=n(input).replace(" ","");val sides=s.split("=");if(sides.size!=2)return fail(input,"Нужно одно равенство.")
        val right=sides[1].toDoubleOrNull() ?: return fail(input,"Пример: (x+1)/(x-2)=3")
        val m=Regex("^\\(([^()]*)\\)/\\(([^()]*)\\)$").matchEntire(sides[0]) ?: Regex("^([^/]+)/\\(([^()]*)\\)$").matchEntire(sides[0]) ?: return fail(input,"Пример: (x+1)/(x-2)=3")
        val num=parsePoly(m.groupValues[1])?:return fail(input,"Не удалось разобрать числитель.");val den=parsePoly(m.groupValues[2])?:return fail(input,"Не удалось разобрать знаменатель.")
        if(degree(num)>1||degree(den)>1)return fail(input,"В первой версии дробное уравнение поддерживает линейные числитель и знаменатель.")
        val forbidden=if(degree(den)==1) -den[0]/den[1] else Double.NaN
        val eq=DoubleArray(2);for(i in 0..1)eq[i]=num.getOrElse(i){0.0}-right*den.getOrElse(i){0.0}
        val base=solveCoefficients(input,eq,listOf("ОДЗ: знаменатель не равен нулю${if(forbidden.isFinite())", x ≠ ${f(forbidden)}" else ""}.","Умножаем обе части на знаменатель."),"Уравнение с дробями")
        val valid=base.numericAnswers.filter{!forbidden.isFinite()||abs(it-forbidden)>EPS};return if(valid.size==base.numericAnswers.size)base else base.copy(answer="Решений нет после проверки ОДЗ",numericAnswers=emptyList(),steps=base.steps+"Исключаем значение, нарушающее ОДЗ.")
    }

    private fun root(input:String):SolveResult {
        val s=n(input).replace("√","sqrt").replace(" ","");val sides=s.split("=");if(sides.size!=2)return fail(input,"Пример: sqrt(2x+3)=5")
        val rhs=sides[1].toDoubleOrNull()?:return fail(input,"Правая часть должна быть числом.");if(rhs<0)return SolveResult(true,"Уравнение с корнем",input,"Решений нет",listOf("Квадратный корень не может быть равен отрицательному числу."))
        val m=Regex("^sqrt\\((.+)\\)$").matchEntire(sides[0])?:return fail(input,"Пример: sqrt(2x+3)=5");val inside=parsePoly(m.groupValues[1])?:return fail(input,"Под корнем нужен линейный многочлен.");if(degree(inside)>1)return fail(input,"В первой версии под корнем поддерживается линейное выражение.")
        val eq=inside.copyOf(max(2,inside.size));eq[0]-=rhs*rhs
        val base=solveCoefficients(input,eq,listOf("ОДЗ: ${m.groupValues[1]} ≥ 0.","Возводим обе части в квадрат: ${m.groupValues[1]} = ${f(rhs*rhs)}."),"Уравнение с корнем")
        val valid=base.numericAnswers.filter{eval(inside,it)>=-EPS};return base.copy(answer=if(valid.isEmpty())"Решений нет после проверки" else "x = ${f(valid[0])}",numericAnswers=valid,steps=base.steps+"Проверяем найденный корень в исходном уравнении.")
    }

    private fun exponential(input:String):SolveResult {
        val s=n(input).replace(" ","");val sides=s.split("=");if(sides.size!=2)return fail(input,"Пример: 2^(3x-1)=16")
        val rhs=sides[1].toDoubleOrNull()?:return fail(input,"Правая часть должна быть числом.");val m=Regex("^([0-9.]+)\\^\\((.+)\\)$").matchEntire(sides[0])?:Regex("^([0-9.]+)\\^(.+)$").matchEntire(sides[0])?:return fail(input,"Пример: 2^(3x-1)=16")
        val b=m.groupValues[1].toDouble();if(b<=0||abs(b-1)<EPS||rhs<=0)return fail(input,"Основание >0 и ≠1, правая часть >0.");val exp=parsePoly(m.groupValues[2])?:return fail(input,"Показатель должен быть линейным.");if(degree(exp)!=1)return fail(input,"Показатель должен быть линейным.");val target=ln(rhs)/ln(b);val x=(target-exp[0])/exp[1]
        return SolveResult(true,"Показательное уравнение",input,"x = ${f(x)}",listOf("Логарифмируем обе части.","${m.groupValues[2]} = ln(${f(rhs)}) / ln(${f(b)}) = ${f(target)}.","Решаем линейное уравнение: x = ${f(x)}."),listOf(x))
    }

    private fun logarithm(input:String):SolveResult {
        val s=n(input).replace(" ","").lowercase();val sides=s.split("=");if(sides.size!=2)return fail(input,"Пример: log2(x+1)=3 или ln(x)=1")
        val rhs=sides[1].toDoubleOrNull()?:return fail(input,"Правая часть должна быть числом.");var base=E;var inside="";val lm=Regex("^ln\\((.+)\\)$").matchEntire(sides[0]);if(lm!=null)inside=lm.groupValues[1] else {val m=Regex("^log_?([0-9.]+)\\((.+)\\)$").matchEntire(sides[0])?:return fail(input,"Пример: log2(x+1)=3");base=m.groupValues[1].toDouble();inside=m.groupValues[2]}
        if(base<=0||abs(base-1)<EPS)return fail(input,"Основание логарифма >0 и ≠1.");val p=parsePoly(inside)?:return fail(input,"Аргумент должен быть линейным.");if(degree(p)!=1)return fail(input,"В первой версии аргумент должен быть линейным.");val target=base.pow(rhs);val x=(target-p[0])/p[1];val st=listOf("ОДЗ: $inside > 0.","По определению логарифма: $inside = ${f(base)}^${f(rhs)} = ${f(target)}.","Решаем линейное уравнение: x = ${f(x)}.","Проверяем ОДЗ — подходит.")
        return SolveResult(true,"Логарифмическое уравнение",input,"x = ${f(x)}",st,listOf(x))
    }

    private fun n(s:String)=s.replace('−','-').replace('–','-').replace('—','-').replace('×','*').replace('·','*').replace(',', '.').replace("²","^2").replace("³","^3")
    private fun parseEquation(s:String):DoubleArray?{val a=n(s).replace(" ","").split("=");if(a.size!=2)return null;val l=parsePoly(a[0])?:return null;val r=parsePoly(a[1])?:return null;val o=DoubleArray(max(l.size,r.size));for(i in o.indices)o[i]=l.getOrElse(i){0.0}-r.getOrElse(i){0.0};return trim(o)}
    private fun parsePoly(raw:String):DoubleArray?{var s=raw.replace("*","");if(s.isBlank()||s.contains('(')||s.contains(')')||s.contains('/'))return null;if(s[0]!='+'&&s[0]!='-')s="+$s";val o=DoubleArray(4);var count=0;for(m in Regex("([+-])([^+-]+)").findAll(s)){count++;val sg=if(m.groupValues[1]=="-")-1.0 else 1.0;val t=m.groupValues[2];val ix=t.indexOf('x');if(ix<0)o[0]+=sg*(t.toDoubleOrNull()?:return null) else {val c=t.substring(0,ix).ifEmpty{"1"}.toDoubleOrNull()?:return null;var p=1;if(ix<t.lastIndex){val q=t.substring(ix+1);if(!q.startsWith("^"))return null;p=q.drop(1).toIntOrNull()?:return null};if(p !in 0..3)return null;o[p]+=sg*c}};return if(count==0)null else trim(o)}
    private fun parseLinear(raw:String,vars:List<Char>):DoubleArray?{val sides=raw.replace(" ","").split("=");if(sides.size!=2)return null;fun side(s0:String):DoubleArray?{var s=s0.replace("*","");if(s.isBlank())return null;if(s[0]!='+'&&s[0]!='-')s="+$s";val o=DoubleArray(vars.size+1);for(m in Regex("([+-])([^+-]+)").findAll(s)){val sg=if(m.groupValues[1]=="-")-1.0 else 1.0;val t=m.groupValues[2];val idx=vars.indexOfFirst{t.endsWith(it)};if(idx>=0)o[idx]+=sg*(t.dropLast(1).ifEmpty{"1"}.toDoubleOrNull()?:return null) else o[vars.size]+=sg*(t.toDoubleOrNull()?:return null)};return o};val l=side(sides[0])?:return null;val r=side(sides[1])?:return null;val o=DoubleArray(vars.size+1);for(i in vars.indices)o[i]=l[i]-r[i];o[vars.size]=r[vars.size]-l[vars.size];return o}
    private fun degree(p:DoubleArray):Int{for(i in p.lastIndex downTo 0)if(abs(p[i])>EPS)return i;return 0}
    private fun trim(p:DoubleArray)=p.copyOf(max(1,degree(p)+1))
    private fun eval(p:DoubleArray,x:Double):Double{var r=0.0;for(i in p.indices.reversed())r=r*x+p[i];return r}
    private fun polyText(p:DoubleArray):String{val z=mutableListOf<String>();for(i in degree(p) downTo 0){val c=p.getOrElse(i){0.0};if(abs(c)<EPS)continue;val a=abs(c);val body=when(i){0->f(a);1->(if(abs(a-1)<EPS)"" else f(a))+"x";else->(if(abs(a-1)<EPS)"" else f(a))+"x^$i"};z+=(if(z.isEmpty()){if(c<0)"-" else ""}else{if(c<0)" - " else " + "})+body};return if(z.isEmpty())"0" else z.joinToString("")}
    private fun sign(v:Double)=if(v<0)"− ${f(abs(v))}" else "+ ${f(v)}"
    fun f(v:Double):String{val r=round(v*1_000_000)/1_000_000;val i=r.toLong();return if(abs(r-i)<EPS)i.toString() else String.format(java.util.Locale.US,"%.6f",r).trimEnd('0').trimEnd('.')}
    private fun fail(i:String,m:String)=SolveResult(false,"",i,"",emptyList(),error=m)
}
