package me.mrletsplay.simplehttpserver.dom.html.element;

import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;

public class HtmlOption extends HtmlElement {

	public HtmlOption() {
		super("option");
	}

	public void setValue(Supplier<String> value) {
		setAttribute("value", value);
	}

	public void setValue(String value) {
		setAttribute("value", value);
	}

	public Supplier<String> getValue() {
		return getAttribute("value");
	}

	@Override
	protected HtmlOption copy(boolean deep) {
		HtmlOption e = new HtmlOption();
		applyAttributes(e, deep);
		return e;
	}

}
