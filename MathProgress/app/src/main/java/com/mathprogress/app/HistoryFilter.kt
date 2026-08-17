package com.mathprogress.app

import java.util.Calendar

data class HistoryFilterState(
    var period: String = "all",
    var source: String = "all",
    var result: String = "all",
    var mode: String = "all",
    var type: String = "all",
    var sort: String = "newest",
    var search: String = "",
    var startAt: Long = 0L,
    var endAt: Long = Long.MAX_VALUE
) {
    fun isDefault(): Boolean = period=="all" && source=="all" && result=="all" && mode=="all" && type=="all" && sort=="newest" && search.isBlank()

    fun apply(input: List<TaskRecord>, now: Long = System.currentTimeMillis()): List<TaskRecord> {
        val (from,to)=range(period,now)
        var list=input.filter { it.createdAt in from..to }
        if(source!="all") list=list.filter { sourceOf(it)==source }
        if(result!="all") list=list.filter { when(result){"correct"->it.checked&&it.correct;"wrong"->it.checked&&!it.correct;"unchecked"->!it.checked;else->true} }
        if(mode!="all") list=list.filter { if(mode=="self")it.selfSolved else !it.selfSolved }
        if(type!="all") list=list.filter { it.type==type }
        if(search.isNotBlank()) { val q=search.trim().lowercase(); list=list.filter { it.input.lowercase().contains(q)||it.answer.lowercase().contains(q)||it.type.lowercase().contains(q) } }
        return when(sort){
            "oldest"->list.sortedBy{it.createdAt}
            "gradeHigh"->list.sortedWith(compareByDescending<TaskRecord>{it.grade}.thenByDescending{it.createdAt})
            "gradeLow"->list.sortedWith(compareBy<TaskRecord>{it.grade}.thenByDescending{it.createdAt})
            "type"->list.sortedWith(compareBy<TaskRecord>{it.type}.thenByDescending{it.createdAt})
            "source"->list.sortedWith(compareBy<TaskRecord>{sourceOf(it)}.thenByDescending{it.createdAt})
            else->list.sortedByDescending{it.createdAt}
        }
    }

    private fun range(period:String,now:Long):Pair<Long,Long>{
        if(period=="custom"||period=="specific")return startAt to endAt
        if(period=="all")return 0L to Long.MAX_VALUE
        val c=Calendar.getInstance().apply{timeInMillis=now;set(Calendar.HOUR_OF_DAY,0);set(Calendar.MINUTE,0);set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)}
        return when(period){
            "today"->c.timeInMillis to c.timeInMillis+86_399_999L
            "yesterday"->{c.add(Calendar.DAY_OF_YEAR,-1);c.timeInMillis to c.timeInMillis+86_399_999L}
            "week"->{val d=(c.get(Calendar.DAY_OF_WEEK)+5)%7;c.add(Calendar.DAY_OF_YEAR,-d);c.timeInMillis to now}
            "month"->{c.set(Calendar.DAY_OF_MONTH,1);c.timeInMillis to now}
            "year"->{c.set(Calendar.DAY_OF_YEAR,1);c.timeInMillis to now}
            else->0L to Long.MAX_VALUE
        }
    }

    fun sourceOf(t:TaskRecord):String=when{t.source.startsWith("daily:")->"daily";t.source=="practice"->"practice";else->"solver"}
}
