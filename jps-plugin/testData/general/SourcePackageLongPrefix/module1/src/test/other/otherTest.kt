package test.other

import bad.prefix.KotlinTestInBadPrefix
import good.prefix.KotlinTestInGoodPrefix
import good.prefix.JavaTest;

val goodTest = KotlinTestInGoodPrefix()
val badTest = KotlinTestInBadPrefix()
val javaTest = JavaTest().bar()
val more = good.prefix.even.deeper.JavaWithInner.B.C()

class Other : good.prefix.even.deeper.JavaWithInner.B.C()