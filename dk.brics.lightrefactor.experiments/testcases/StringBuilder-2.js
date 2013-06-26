function StringBuilder() {
	this.clear()
}
StringBuilder.prototype.clear = function() {
	this.array /*1*/ = []
}
StringBuilder.prototype.append = function(x) {
	this.array /*1*/.push(x)
}
StringBuilder.prototype.toString = function() {
	return this.array /*1*/.join('')
}

var sb = new StringBuilder;

sb.append("dfg")
sb.clear()
sb.toString()


function UnrelatedClass() {
	this.array /*1*/ = []
}
UnrelatedClass.prototype.append = function(x) {
	this.array /*1*/.push(x)
}

var x = new StringBuilder() || new UnrelatedClass();
x.array /*1*/ = [];
