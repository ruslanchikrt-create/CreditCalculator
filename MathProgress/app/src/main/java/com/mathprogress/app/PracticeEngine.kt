package com.mathprogress.app

import kotlin.math.abs
import kotlin.random.Random

data class PracticeQuestion(
    val input: String,
    val type: String,
    val expected: List<Double>,
    val answerText: String,
    val difficulty: Int = 1
)

object PracticeEngine {
    private val topics = listOf("Линейные", "Квадратные", "Кубические", "Системы 2×2", "Дроби", "Корни", "Показательные", "Логарифмические")
    private val difficultyRu = listOf("Лёгкий", "Средний", "Сложный", "Эксперт")
    fun topics(): List<String> = topics
    fun difficultyNames(): List<String> = difficultyRu + "Адаптивный"
    fun difficultyName(level: Int): String = difficultyRu[(level.coerceIn(1,4))-1]

    fun suggestedDifficulty(tasks: List<TaskRecord>): Int {
        val checked = tasks.filter { it.checked }.sortedByDescending { it.createdAt }.take(20)
        if (checked.size < 5) return 1
        val acc = checked.count { it.correct }.toDouble() / checked.size
        val avgLevel = checked.map { it.difficulty.coerceIn(1,4) }.average()
        return when {
            acc >= .9 && avgLevel >= 3.0 -> 4
            acc >= .85 && avgLevel >= 2.0 -> 3
            acc >= .75 -> 2
            else -> 1
        }
    }

    fun generate(topic: String, difficulty: Int = 1, seed: Long? = null): PracticeQuestion {
        val level = difficulty.coerceIn(1,4)
        val r = if (seed == null) Random(System.nanoTime()) else Random(seed)
        val range = when(level){1->6;2->10;3->15;else->24}
        return when(topic) {
            "Квадратные" -> {
                val a = when(level){1->1;2->r.nextInt(1,3);3->r.nextInt(2,5);else->r.nextInt(2,7)}
                val x1 = r.nextInt(-range,range+1); var x2 = r.nextInt(-range,range+1); if (x2==x1) x2 += 1
                val b = -a*(x1+x2); val c = a*x1*x2
                val eq = "${coef(a)}x^2${signed(b,"x")}${signed(c,"")}=0"
                PracticeQuestion(eq,"Квадратное уравнение",listOf(x1.toDouble(),x2.toDouble()),"x = $x1 и x = $x2",level)
            }
            "Кубические" -> {
                val rootRange = when(level){1->4;2->6;3->8;else->10}
                val roots=(-rootRange..rootRange).shuffled(r).take(3); val a=roots[0]; val b=roots[1]; val c=roots[2]
                val s1=a+b+c; val s2=a*b+a*c+b*c; val s3=a*b*c
                val mult = if(level>=4) r.nextInt(2,4) else 1
                PracticeQuestion("${coef(mult)}x^3${signed(-mult*s1,"x^2")}${signed(mult*s2,"x")}${signed(-mult*s3,"")}=0","Кубическое уравнение",listOf(a.toDouble(),b.toDouble(),c.toDouble()),"x = $a, $b, $c",level)
            }
            "Системы 2×2" -> {
                val x=r.nextInt(-range,range+1); val y=r.nextInt(-range,range+1)
                val maxCoef=when(level){1->4;2->6;3->9;else->12}
                val a=r.nextInt(1,maxCoef); val b=r.nextInt(1,maxCoef); val c=r.nextInt(1,maxCoef); var d=r.nextInt(-maxCoef,maxCoef+1).let{if(it==0)1 else it}
                if (a*d-b*c==0) d += 1
                val e=a*x+b*y; val f=c*x+d*y
                val text="${coef(a)}x${signed(b,"y")}=$e; ${coef(c)}x${signed(d,"y")}=$f"
                PracticeQuestion(text,"Система 2×2",listOf(x.toDouble(),y.toDouble()),"x = $x, y = $y",level)
            }
            "Дроби" -> {
                var target=r.nextInt(-range,range+1); val b=r.nextInt(1,when(level){1->5;2->8;3->11;else->15}); if (target == -b) target += 1
                val k=r.nextInt(2,when(level){1->5;2->7;3->9;else->12}); val a = k*(target+b)-target
                PracticeQuestion("(x${signed(a,"")})/(x${signed(b,"")})=$k","Уравнение с дробями",listOf(target.toDouble()),"x = $target",level)
            }
            "Корни" -> {
                val x=r.nextInt(0,range+1); val coef=when(level){1->2;2->r.nextInt(2,5);3->r.nextInt(3,7);else->r.nextInt(4,9)}
                val rhs=r.nextInt(2,when(level){1->8;2->10;3->13;else->16}); val c=rhs*rhs-coef*x
                PracticeQuestion("sqrt(${coef}x${signed(c,"")})=$rhs","Уравнение с корнем",listOf(x.toDouble()),"x = $x",level)
            }
            "Показательные" -> {
                val base=listOf(2,3,5).random(r); val x=r.nextInt(1,when(level){1->5;2->6;3->7;else->8}); val rhs=powInt(base,x)
                if(level<=2) PracticeQuestion("$base^x=$rhs","Показательное уравнение",listOf(x.toDouble()),"x = $x",level)
                else {
                    val a=if(level==3)2 else r.nextInt(2,5); val b=r.nextInt(-4,5); val power=a*x+b; val target=powInt(base,power.coerceAtLeast(0))
                    PracticeQuestion("$base^(${a}x${signed(b,"")})=$target","Показательное уравнение",listOf(x.toDouble()),"x = $x",level)
                }
            }
            "Логарифмические" -> {
                val base=listOf(2,3,5).random(r); val power=r.nextInt(1,when(level){1->4;2->5;3->6;else->7}); val offset=r.nextInt(1,range.coerceAtLeast(2)); val x=powInt(base,power)-offset
                PracticeQuestion("log$base(x+$offset)=$power","Логарифмическое уравнение",listOf(x.toDouble()),"x = $x",level)
            }
            else -> {
                val x=r.nextInt(-range,range+1); val a=r.nextInt(1,when(level){1->8;2->12;3->18;else->25}); val b=r.nextInt(-range*2,range*2+1); val c=a*x+b
                PracticeQuestion("${coef(a)}x${signed(b,"")}=$c","Линейное уравнение",listOf(x.toDouble()),"x = $x",level)
            }
        }
    }

    fun dailyQuestions(dateKey: String, profileId: String, attempt: Int, count: Int = 5, baseDifficulty: Int = 1): List<PracticeQuestion> {
        val offset = abs((dateKey + profileId).hashCode()) % topics.size
        val level = (baseDifficulty + if(attempt>=3)1 else 0).coerceIn(1,4)
        return (0 until count).map { index ->
            val topic = topics[(offset + index) % topics.size]
            val seed = "$dateKey|$profileId|$attempt|$index|$level".hashCode().toLong() * 7919L
            generate(topic, difficulty = level, seed = seed)
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
    private fun signed(v:Int, suffix:String)=if(v<0)"-${abs(v)}$suffix" else if(v>0)"+$v$suffix" else ""
    private fun powInt(a:Int,b:Int):Int { var out=1; repeat(b.coerceAtLeast(0)){out*=a}; return out }
}
