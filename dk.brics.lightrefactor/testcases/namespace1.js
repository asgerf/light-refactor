var util = util || {};

util.Vector2 = function(x,y) {
	this.x = x;
	this.y = y;
}
util.Rect = function(x,y,w,h) {
	this.x = x;
	this.y = y;
	this.width = w;
	this.height = h;
}

var v = new util.Vector2(0, 10);
var r = new util.Rect(10, 10, 100, 100);

