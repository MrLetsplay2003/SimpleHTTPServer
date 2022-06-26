package me.mrletsplay.simplehttpserver.dom.html.element;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;

public class HtmlSelect extends HtmlElement {

	public HtmlSelect() {
		super("select");
	}

	public void addOption(String name, String value) {
		HtmlOption op = new HtmlOption();
		op.setText(name);
		op.setValue(value);
		appendChild(op);
	}

	public void addOption(String name, String value, boolean selected) {
		HtmlOption op = new HtmlOption();
		op.setText(name);
		op.setValue(value);
		op.setSelected(selected);
		appendChild(op);
	}

	@Override
	protected HtmlSelect copy(boolean deep) {
		HtmlSelect e = new HtmlSelect();
		applyAttributes(e, deep);
		return e;
	}

}
