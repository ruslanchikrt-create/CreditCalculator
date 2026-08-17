package com.mathprogress.app

import kotlin.random.Random

data class PracticeQuestion(val input: String, val type: String, val expected: List<Double>, val answerText: String)

object PracticeEngine {
    private val topics = listOf("Линейные", "Квадратные", "Кубические", "Системы 2×2", "Дроби", "Корни", "Показательные", "Логарифмические")
    fun topics(): List<String> = topics

    fun generate(topic: String, difficulty: Int = 1, seed: Long? = null): PracticeQuestion {
        val r = if (seed == null) Random(System.nanoTime()) else Random(seed)
        return when(topic) {
            "Квадратные" -> {
                val a = if (difficulty >= 3) r.nextInt(1,4) else 1
                val x1 = r.nextInt(-6,7); var x2 = r.nextInt(-6,7); if (x2==x1) x2 += 1
                val b = -a*(x1+x2); val c = a*x1*x2
                val eq = "${coef(a)}x^2${signed(b,"x")}${signed(c,"")}=0"
                PracticeQuestion(eq,"Квадратное уравнение",listOf(x1.toDouble(),x2.toDouble()),"x = $x1 и x = $x2")
            }
            "Кубические" -> {
                val roots=(-4..4).shuffled(r).take(3); val a=roots[0]; val b=roots[1]; val c=roots[2]
                val s1=a+b+c; val s2=a*b+a*c+b*c; val s3=a*b*c
                PracticeQuestion("x^3${signed(-s1,"x^2")}${signed(s2,"x")}${signed(-s3,"")}=0","Кубическое уравнение",listOf(a.toDouble(),b.toDouble(),c.toDouble()),"x = $a, $b, $c")
            }
            "Системы 2×2" -> {
                val x=r.nextInt(-5,6); val y=r.nextInt(-5,6)
                val a=r.nextInt(1,5); val b=r.nextInt(1,5); val c=r.nextInt(1,5); var d=r.nextInt(-4,5).let{if(it==0)1 else it}
                if (a*d-b*c==0) d += 1
                val e=a*x+b*y; val f=c*x+d*y
                val text="${coef(a)}x${signed(b,"y")}=$e; ${coef(c)}x${signed(d,"y")}=$f"
                PracticeQuestion(text,"Система 2×2",listOf(x.toDouble(),y.toDouble()),"x = $x, y = $y")
            }
            "Дроби" -> {
                var target=r.nextInt(-5,6); val b=r.nextInt(1,5); if (target == -b) target += 1; val k=r.nextInt(2,5)
                val a = k*(target+b)-target
                val eq="(x${signed(a,"")})/(x${signed(b,"")})=$k"
                PracticeQuestion(eq,"Уравнение с дробями",listOf(target.toDouble()),"x = $target")
            }
            "Корни" -> {
                val x=r.nextInt(0,8); val rhs=r.nextInt(2,8); val c=rhs*rhs-2*x
                val eq="sqrt(2x${signed(c,"")})=$rhs"
                PracticeQuestion(eq,"Уравнение с корнем",listOf(x.toDouble()),"x = $x")
            }
            "Показательные" -> {
                val base=listOf(2,3,5).random(r); val x=r.nextInt(1,5); val rhs=powInt(base,x)
                PracticeQuestion("$base^x=$rhs","Показательное уравнение",listOf(x.toDouble()),"x = $x")
            }
            "Логарифмические" -> {
                val base=listOf(2,3,5).random(r); val power=r.nextInt(1,4); val offset=r.nextInt(1,5); val x=powInt(base,power)-offset
                PracticeQuestion("log$base(x+$offset)=$power","Логарифмическое уравнение",listOf(x.toDouble()),"x = $x")
            }
            else -> {
                val x=r.nextInt(-10,11); val a=r.nextInt(1,8); val b=r.nextInt(-10,11); val c=a*x+b
                PracticeQuestion("${coef(a)}x${signed(b,"")}=$c","Линейное уравнение",listOf(x.toDouble()),"x = $x")
            }
        }
    }

    fun dailyQuestions(dateKey: String, profileId: String, attempt: Int, count: Int = 5): List<PracticeQuestion> {
        val offset = kotlin.math.abs((dateKey + profileId).hashCode()) % topics.size
        return (0 until count).map { index ->
            val topic = topics[(offset + index) % topics.size]
            val seed = "$dateKey|$profileId|$attempt|$index".hashCode().toLong() * 7919L
            generate(topic, difficulty = 1 + (attempt.coerceAtMost(3) - 1), seed = seed)
        }
    }

    fun check(userInput: String, expected: List<Double>): Boolean {
        val nums = Regex("(?<![A-Za-zА-Яа-я_])[-+]?\\d+(?:[.,]\\d+)?").findAll(userInput.replace(',', '.')).mapNotNull { it.value.toDoubleOrNull() }.toList()
        if (nums.size < expected.size) return false
        val remaining = nums.toMutableList()
        for (e in expected) {
            val idx = remaining.indexOfFirst { kotlin.math.abs(it-e) < 1e-4 }
            if (idx < 0) return false
            remaining.removeAt(idx)
        }
        return true
    }

    fun grade(correct: Int, total: Int): Int {
        if (total <= 0) return 0
        val p=correct*100.0/total
        return when { p>=90->5; p>=75->4; p>=60->3; else->2 }
    }

    private fun coef(v:Int)=when(v){1->"";-1->"-";else->v.toString()}
    private fun signed(v:Int, suffix:String)=if(v<0)"-${kotlin.math.abs(v)}$suffix" else if(v>0)"+$v$suffix" else ""
    private fun powInt(a:Int,b:Int):Int { var r=1; repeat(b){r*=a}; return r }
}
