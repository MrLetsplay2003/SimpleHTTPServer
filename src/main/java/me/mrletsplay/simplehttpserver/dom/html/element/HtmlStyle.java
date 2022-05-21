package me.mrletsplay.simplehttpserver.dom.html.element;

import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;
import me.mrletsplay.simplehttpserver.dom.html.HtmlElementFlag;

public class HtmlStyle extends HtmlElement {

	public HtmlStyle() {
		super("style");
		flags.add(HtmlElementFlag.DONT_ESCAPE_TEXT);
	}

	public void setMedia(Supplier<String> media) {
		setAttribute("media", media);
	}

	public void setMedia(String media) {
		setAttribute("media", media);
	}

	public Supplier<String> getMedia() {
		return getAttribute("media");
	}

	@Override
	protected HtmlStyle copy(boolean deep) {
		HtmlStyle style = new HtmlStyle();
		applyAttributes(style, deep);
		return style;
	}

}
