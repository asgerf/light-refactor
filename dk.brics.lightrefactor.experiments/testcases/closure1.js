function f() {
	function klass()
	return klass;
}

a = f();
b = f();


a.g /* 1 */ = 5;
b.g /* 2 */ = 6;
