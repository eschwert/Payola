package s2js.compiler

class LiteralSpecs extends CompilerFixtureSpec
{
    describe("Literals") {
        it("null is supported") {
            configMap =>
                scalaCode {
                    """
                        package p
                        object `package`  {
                            def a() {
                                null
                            }
                        }
                    """
                } shouldCompileTo {
                    """
                        s2js.runtime.client.ClassLoader.provide('p');

                        p.a = function() {
                            var self = this;
                            null;
                        };
                        p.__class__ = new s2js.runtime.client.Class('p', []);
                    """
                }
        }

        it("booleans are supported") {
            configMap =>
                scalaCode {
                    """
                        package p
                        object `package` {
                            def a() {
                                true
                                false
                            }
                        }
                    """
                } shouldCompileTo {
                    """
                        s2js.runtime.client.ClassLoader.provide('p');

                        p.a = function() {
                            var self = this;
                            true;
                            false;
                        };
                        p.__class__ = new s2js.runtime.client.Class('p', []);
                    """
                }
        }

        it("numbers are supported") {
            configMap =>
                scalaCode {
                    """
                        package p
                        object `package` {
                            def a() {
                                1234
                                574.432
                                0
                                -5
                                -424.45
                            }
                        }
                    """
                } shouldCompileTo {
                    """
                        s2js.runtime.client.ClassLoader.provide('p');

                        p.a = function() {
                            var self = this;
                            1234;
                            574.432;
                            0;
                            -5;
                            -424.45;
                        };
                        p.__class__ = new s2js.runtime.client.Class('p', []);
                    """
                }
        }

        it("chars are supported") {
            configMap =>
                scalaCode {
                    """
                    object a {
                        val a = 'a'
                        val b = 'b'
                        val c = '\b'
                        val d = '\f'
                        val e = '\n'
                        val f = '\r'
                        val g = '\t'
                        val h = '\''
                        val i = '\"'
                        val j = '\\'
                    }
                    """
                } shouldExactlyCompileTo {
                    """s2js.runtime.client.ClassLoader.provide('a');""" + "\n" +
                    """a.a = 'a';""" + "\n" +
                    """a.b = 'b';""" + "\n" +
                    """a.c = '\b';""" + "\n" +
                    """a.d = '\f';""" + "\n" +
                    """a.e = '\n';""" + "\n" +
                    """a.f = '\r';""" + "\n" +
                    """a.g = '\t';""" + "\n" +
                    """a.h = '\'';""" + "\n" +
                    """a.i = '\"';""" + "\n" +
                    """a.j = '\\';""" + "\n" +
                    """a.__class__ = new s2js.runtime.client.Class('a', []);""" + "\n" +
                    """"""
                }
        }

        it("strings are supported") {
            configMap =>
                scalaCode {
                    """
                        object a {
                            val a = "asdfghjkl"
                            val b = "12345"
                            val c = ""
                            val d = "\b"
                            val f = "\f"
                            val g = "\n"
                            val h = "\r"
                            val i = "\t"
                            val j = "\'"
                            val k = "\""
                            val l = "\\"
                            val m = """ + "\"\"\"" + """multiline""" + "\n" +
                                """string""" + "\n" +
                            "\"\"\"" + """
                        }
                    """
                } shouldExactlyCompileTo {
                    """s2js.runtime.client.ClassLoader.provide('a');""" + "\n" +
                    """a.a = 'asdfghjkl';""" + "\n" +
                    """a.b = '12345';""" + "\n" +
                    """a.c = '';""" + "\n" +
                    """a.d = '\b';""" + "\n" +
                    """a.f = '\f';""" + "\n" +
                    """a.g = '\n';""" + "\n" +
                    """a.h = '\r';""" + "\n" +
                    """a.i = '\t';""" + "\n" +
                    """a.j = '\'';""" + "\n" +
                    """a.k = '\"';""" + "\n" +
                    """a.l = '\\';""" + "\n" +
                    """a.m = 'multiline\nstring\n';""" + "\n" +
                    """a.__class__ = new s2js.runtime.client.Class('a', []);""" + "\n" +
                    """"""
                }
        }
    }
}
