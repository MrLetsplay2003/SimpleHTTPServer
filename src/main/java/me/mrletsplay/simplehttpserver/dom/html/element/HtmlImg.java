package me.mrletsplay.simplehttpserver.dom.html.element;

import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;

public class HtmlImg extends HtmlElement {

	public HtmlImg() {
		super("img");
		setNoClosingTag(true);
	}

	public void setSrc(Supplier<String> src) {
		setAttribute("src", src);
	}

	public void setSrc(String src) {
		setAttribute("src", src);
	}

	public Supplier<String> getSrc() {
		return getAttribute("src");
	}

	public void setAlt(Supplier<String> alt) {
		setAttribute("alt", alt);
	}

	public void setAlt(String alt) {
		setAttribute("alt", alt);
	}

	public Supplier<String> getAlt() {
		return getAttribute("alt");
	}

	@Override
	protected HtmlImg copy(boolean deep) {
		HtmlImg e = new HtmlImg();
		applyAttributes(e, deep);
		return e;
	}

}
