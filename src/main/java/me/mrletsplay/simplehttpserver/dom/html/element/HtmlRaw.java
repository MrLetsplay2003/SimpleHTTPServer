package me.mrletsplay.simplehttpserver.dom.html.element;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;

public class HtmlRaw extends HtmlElement {

	private String raw;

	public HtmlRaw(String raw) {
		super(null);
		this.raw = raw;
	}

	@Override
	protected HtmlRaw copy(boolean deep) {
		return new HtmlRaw(raw);
	}

	@Override
	public String toString() {
		return raw;
	}

}
