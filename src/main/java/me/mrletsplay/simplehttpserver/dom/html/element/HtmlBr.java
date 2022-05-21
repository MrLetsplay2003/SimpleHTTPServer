package me.mrletsplay.simplehttpserver.dom.html.element;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;

public class HtmlBr extends HtmlElement {

	public HtmlBr() {
		super("br");
		setSelfClosing(true);
	}

	@Override
	protected HtmlBr copy(boolean deep) {
		HtmlBr e = new HtmlBr();
		applyAttributes(e, deep);
		return e;
	}

}
