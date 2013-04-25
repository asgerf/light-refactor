var util = (function() {
	function Vector2(x,y) {
		this.x = x;
		this.y = y;
	};
	function Rect(x,y,w,h) {
		this.x = x;
		this.y = y;
		this.width = w;
		this.height = h;
	};
	return {
		Vector2: Vector2,
		Rect: Rect
	};
})();

new util.Vector2(0, 10);
