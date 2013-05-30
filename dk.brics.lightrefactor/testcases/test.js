function foo() {
    for (i=0; i<10; i++) {
        bar();
    }
}
function bar() {
    for (i=0; i<5; i++) {
        // ...
    }
}

function extend(src,dst) {
	for (var x in src) {
		dst[x] = src[x];
	}
}

foo.f = function() {...};

extend(foo,bar);

bar.f();


(function(x,y) {
	/* ... */
})(a,b);

for (var i=0; i<10; i++) {
	button[i].addEvent("click", function() {
		panel[i].activate();
	});
}