package s2js.runtime.client.scala.collection

import s2js.runtime.client.scala.util.control.Breaks._
import s2js.compiler.javascript

trait Seq extends Iterable
{
    @javascript("[]")
    var internalJsArray: AnyRef = null

    def getInternalJsArray = internalJsArray

    def setInternalJsArray(value: AnyRef) {
        internalJsArray = value
    }

    @javascript("""
        for (var i in self.getInternalJsArray()) {
            f(self.getInternalJsArray()[i]);
        }
    """)
    def foreach[U](f: Double => U) {}

    @javascript("self.getInternalJsArray().push(x);")
    def +=(x: Any) {}

    // From TraversableLike
    def reversed: Iterable = {
        val elems: Iterable = newInstance
        for (x <- this) {
            elems.prepend(x)
        }
        elems
    }

    @javascript("return self.getInternalJsArray().length;")
    override def size: Int = 0

    @javascript("""
        if (s2js.runtime.client.isUndefined(self.getInternalJsArray()[n])) {
            throw new scala.NoSuchElementException('An item with index ' + n + ' is not present.');
        }
        return self.getInternalJsArray()[n];
    """)
    def apply(n: Int): Any = null

    @javascript("""
        if (self.size() <= n) {
            throw new scala.NoSuchElementException('An item with index ' + n + ' is not present.');
        }
        self.getInternalJsArray()[n] = newelem;
    """)
    def update(n: Int, newelem: Any) {}

    def length: Int = size

    @javascript("""
        if (index < 0 || self.size() <= index) {
            throw new scala.NoSuchElementException('An item with index ' + n + ' is not present.');
        }
        var removed = self.getInternalJsArray()[index];
        self.getInternalJsArray().splice(index, 1);
        return removed;
    """)
    def remove(index: Int) {}

    @javascript("""self.getInternalJsArray().splice(0, 0, x);""")
    def prepend(x: Any) {}

    @javascript("""
        var index = self.getInternalJsArray().indexOf(x);
        if (index != -1) {
            self.getInternalJsArray().splice(index, 1);
        }
    """)
    def -=(x: Double) {}

    // From SeqLike
    def indexWhere(p: Double => Boolean, from: Int = 0): Int = {
        var i = from
        breakable {() =>
            drop(from).foreach {x =>
                if (p(x)) {
                    break()
                } else {
                    i += 1
                }
            }
            i = -1
        }
        i
    }

    // From SeqLike
    def contains(x: Double): Boolean = {
        exists(_ == x)
    }

    def endsWith(suffix: Seq): Boolean = {
        suffix.length match {
            case suffixLength if suffixLength > length => false
            case 0 => true
            case suffixLength => {
                var result = true
                breakable {() =>
                    val startIndex = length - suffixLength
                    var index = 0
                    suffix.foreach {item =>
                        if (item != this(startIndex + index)) {
                            result = false
                            break()
                        }
                        index += 1
                    }
                }
                result
            }
        }
    }
}

