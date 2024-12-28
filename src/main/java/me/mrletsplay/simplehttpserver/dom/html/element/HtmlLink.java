package me.mrletsplay.simplehttpserver.dom.html.element;

import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;

public class HtmlLink extends HtmlElement {

	public HtmlLink() {
		super("link");
		setNoClosingTag(true);
	}

	public void setRel(Supplier<String> rel) {
		setAttribute("rel", rel);
	}

	public void setRel(String rel) {
		setAttribute("rel", rel);
	}

	public Supplier<String> getRel() {
		return getAttribute("rel");
	}

	public void setHref(Supplier<String> href) {
		setAttribute("href", href);
	}

	public void setHref(String href) {
		setAttribute("href", href);
	}

	public Supplier<String> getHref() {
		return getAttribute("href");
	}

	public void setLinkType(Supplier<String> type) {
		setAttribute("type", type);
	}

	public void setLinkType(String type) {
		setAttribute("type", type);
	}

	public Supplier<String> getLinkType() {
		return getAttribute("type");
	}

	@Override
	protected HtmlLink copy(boolean deep) {
		HtmlLink e = new HtmlLink();
		applyAttributes(e, deep);
		return e;
	}

}
