var util = (function() {
	var util = {};
	
	util.Vector2 = function(x,y) {
		this.x = x;
		this.y = y;
	};
	util.Rect = function(x,y,w,h) {
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
	};
	
	return util;
})();

new util.Vector2(0, 10);
