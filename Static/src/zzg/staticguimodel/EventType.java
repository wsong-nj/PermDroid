package zzg.staticguimodel;

import java.io.Serializable;

public enum EventType implements Serializable {
	CLICK, 
	LONGCLICK, 
	SCROLL,
	DRAG,
	HOVER,
	TOUCH,
	LEFTSLIDE,
	RIGHTSLIDE;
	
	public static EventType getEventType(String methodName) {
		switch (methodName) {
			case "setOnClickListener":
			case "setOnItemClickListener":
				return EventType.CLICK;
			case "setOnLongClickListener":
			case "setOnItemLongClickListener":
				return EventType.LONGCLICK;
			case "setOnScrollListener":
				return EventType.SCROLL;
			case "setOnDragListener":
				return EventType.DRAG;
			case "setOnHoverListener":
				return EventType.HOVER;
			case "setOnTouchListener":
				return EventType.TOUCH;
			default:
				return null;
		}
	}
}
