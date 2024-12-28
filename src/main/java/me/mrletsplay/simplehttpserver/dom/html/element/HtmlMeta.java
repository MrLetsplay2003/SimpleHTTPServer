package me.mrletsplay.simplehttpserver.dom.html.element;

import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;

public class HtmlMeta extends HtmlElement {

	public HtmlMeta() {
		super("meta");
		setNoClosingTag(true);
	}

	public void setName(Supplier<String> name) {
		setAttribute("name", name);
	}

	public void setName(String name) {
		setName(() -> name);
	}

	public void setContent(Supplier<String> content) {
		setAttribute("content", content);
	}

	public void setContent(String content) {
		setContent(() -> content);
	}

	@Override
	protected HtmlMeta copy(boolean deep) {
		HtmlMeta e = new HtmlMeta();
		applyAttributes(e, deep);
		return e;
	}

}
