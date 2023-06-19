package team.jlm.utils.collections

import java.util.*

class UnionFindSet<V> {
    constructor() : this(emptyList())
    constructor(list: List<V>) {
        for (value in list) {
            add(value)
        }
    }

    fun contains(value: V) = elementHashMap.containsKey(value)

    fun add(value: V) {
        elementHashMap[value] = value
        fatherMap[value] = value
        sizeMap[value] = 1
    }

    private var elementHashMap: HashMap<V, V> = HashMap()

    //key 某个元素 value 该元素的父
    private var fatherMap: HashMap<V, V> = HashMap()

    // key 某个集合的代表元素， value 该集合的大小
    private var sizeMap: HashMap<V, Int> = HashMap()

    fun union(a: V, b: V) {
        if (elementHashMap.containsKey(a) && elementHashMap.containsKey(b)) {
            val headA = elementHashMap[a]!!
            val headB = elementHashMap[b]!!
            if (headA !== headB) {
                val big = if (sizeMap[headA]!! > sizeMap[headB]!!) headA else headB
                val small = if (big === headA) headB else headA
                fatherMap[small] = big
                sizeMap[big] = sizeMap[big]!! + sizeMap[small]!!
                sizeMap.remove(small)
            }
        }
    }

    fun isInSameSet(a: V, b: V): Boolean {
        return if (elementHashMap.containsKey(a) && elementHashMap.containsKey(b)) {
            getHead(elementHashMap[a]!!) === getHead(elementHashMap[b]!!)
        } else false
    }

    fun getHead(e: V): V? {
        var result = e
        val stack = Stack<V>()
        while (result !== fatherMap[result]) {
            stack.push(result)
            result = fatherMap[result] ?: return null
        }
        //此时e就是头顶节点
        while (!stack.isEmpty()) {
            fatherMap[stack.pop()] = result
        }
        return result
    }
}
