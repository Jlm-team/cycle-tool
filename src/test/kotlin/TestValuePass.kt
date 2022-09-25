import org.junit.Test

class T1 {
    var a = 1
}

class TestValuePass {
    fun test1(t1: T1) {
        t1.a = 2
    }

    @Test
    fun test() {
        val t = T1()
        assert(t.a == 1)
        test1(t)
        assert(t.a == 2)
    }
}