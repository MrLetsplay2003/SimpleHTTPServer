package me.mrletsplay.simplehttpserver.dom.html.element;

import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;
import me.mrletsplay.simplehttpserver.dom.html.HtmlElementFlag;

public class HtmlScript extends HtmlElement {

	public HtmlScript() {
		super("script");
		flags.add(HtmlElementFlag.DONT_ESCAPE_TEXT);
	}

	public void setSource(Supplier<String> src) {
		setAttribute("src", src);
	}

	public void setSource(String src) {
		setAttribute("src", src);
	}

	public Supplier<String> getSource() {
		return getAttribute("src");
	}

	public void setAsync(boolean async) {
		if(async) {
			setAttribute("async");
		}else {
			unsetAttribute("async");
		}
	}

	public void setDefer(boolean defer) {
		if(defer) {
			setAttribute("defer");
		}else {
			unsetAttribute("defer");
		}
	}

	@Override
	protected HtmlScript copy(boolean deep) {
		HtmlScript e = new HtmlScript();
		applyAttributes(e, deep);
		return e;
	}

}
