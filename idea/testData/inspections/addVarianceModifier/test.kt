interface EffectivelyOut<T> {
    fun foo(): T
    val bar: T
}
interface EffectivelyIn<T> {
    fun foo(arg: T)
}
interface Invariant<T> {
    var bar: T
}