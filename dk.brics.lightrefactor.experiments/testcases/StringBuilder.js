function StringBuilder() {
	this.clear()
}
StringBuilder.prototype.clear = function() {
	this.array = []
}
StringBuilder.prototype.append = function(x) {
	this.array.push(x)
}
StringBuilder.prototype.toString = function() {
	return this.array.join('')
}

var sb = new StringBuilder;

sb.append("dfg")
sb.clear()
sb.toString()


function UnrelatedClass() {
	this.array = []
}
UnrelatedClass.prototype.append = function(x) {
	this.array.push(x)
}
