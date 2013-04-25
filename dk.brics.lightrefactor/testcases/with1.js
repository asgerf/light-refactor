var obj = {
		foo: 5
};

function f(o) {
	var foo = 4;
	with (o) {
		return foo;
	}
}

f(obj);
