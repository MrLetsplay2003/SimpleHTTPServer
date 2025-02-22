package me.mrletsplay.simplehttpserver.dom.html;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.text.StringEscapeUtils;

import me.mrletsplay.simplehttpserver.dom.css.StyleSheet;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlBr;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlButton;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlImg;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlLink;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlMeta;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlOption;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlScript;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlSelect;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlStyle;
import me.mrletsplay.simplehttpserver.dom.js.JSScript;

public class HtmlElement {

	// Source: https://html.spec.whatwg.org/multipage/syntax.html#void-elements
	public static final Set<String> VOID_ELEMENTS = Set.of(
		"area",
		"base",
		"br",
		"col",
		"embed",
		"hr",
		"img",
		"input",
		"link",
		"meta",
		"source",
		"track",
		"wbr"
	);

	private HtmlElement parent;
	private String type;
	private Supplier<String> text;
	private Map<String, Supplier<String>> attributes;
	private List<HtmlElement> children;
	private boolean noClosingTag;
	protected EnumSet<HtmlElementFlag> flags;

	@Deprecated
	public HtmlElement(String type) {
		this.type = type;
		this.attributes = new HashMap<>();
		this.children = new ArrayList<>();
		this.flags = EnumSet.noneOf(HtmlElementFlag.class);
	}

	public HtmlElement getParent() {
		return parent;
	}

	public String getType() {
		return type;
	}

	public void setText(Supplier<String> text) {
		this.text = text;
	}

	public void setText(String text) {
		setText(() -> text);
	}

	public Supplier<String> getText() {
		return text;
	}

	public void setAttribute(String name, Supplier<String> value) {
		attributes.put(name, value);
	}

	public void setAttribute(String name, String value) {
		setAttribute(name, () -> value);
	}

	public void appendAttribute(String name, Supplier<String> value) {
		Supplier<String> prevVal = getAttribute(name);
		if(prevVal == null) {
			setAttribute(name, value);
			return;
		}
		attributes.put(name, () -> prevVal.get() + value.get());
	}

	public void appendAttribute(String name, String value) {
		appendAttribute(name, () -> value);
	}

	public void setAttribute(String name) {
		setAttribute(name, (String) null);
	}

	public void unsetAttribute(String name) {
		attributes.remove(name);
	}

	public Supplier<String> getAttribute(String name) {
		return attributes.get(name);
	}

	public Map<String, Supplier<String>> getAttributes() {
		return attributes;
	}

	public void appendChild(HtmlElement child) throws HtmlException {
		if(child.getParent() != null) child.getParent().removeChild(child);
		HtmlElement tmpC = this;
		while(tmpC.getParent() != null) {
			tmpC = tmpC.getParent();
			if(tmpC == this) throw new HtmlException("Impossible circular hierarchy");
		}
		children.add(child);
		child.parent = this;
	}

	public void removeChild(HtmlElement child) {
		children.remove(child);
	}

	public List<HtmlElement> getChildren() {
		return children;
	}

	public void setNoClosingTag(boolean noClosingTag) {
		this.noClosingTag = noClosingTag;
	}

	public boolean isNoClosingTag() {
		return noClosingTag;
	}

	public void setID(Supplier<String> id) {
		setAttribute("id", id);
	}

	public void setID(String id) {
		setAttribute("id", id);
	}

	public Supplier<String> getID() {
		return getAttribute("id");
	}

	public void addClass(Supplier<String> htmlClass) {
		Supplier<String> oldC = getAttribute("class");
		if(oldC == null) {
			setAttribute("class", htmlClass);
			return;
		}
		setAttribute("class", () -> oldC.get() + " " + htmlClass.get());
	}

	public void addClass(String htmlClass) {
		addClass(() -> htmlClass);
	}

	public EnumSet<HtmlElementFlag> getFlags() {
		return flags;
	}

	public HtmlElement copy() {
		return copy(false);
	}

	public HtmlElement deepCopy() {
		return copy(true);
	}

	protected HtmlElement copy(boolean deep) {
		HtmlElement el = new HtmlElement(type);
		applyAttributes(el, deep);
		return el;
	}

	protected void applyAttributes(HtmlElement copy, boolean deep) {
		copy.type = type;
		copy.text = text;
		copy.attributes = new HashMap<>(attributes);
		if(deep) {
			for(HtmlElement child : children) {
				copy.appendChild(child.deepCopy());
			}
		}
		copy.noClosingTag = noClosingTag;
		copy.flags = EnumSet.copyOf(flags);
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("<").append(getType());
		for(Map.Entry<String, Supplier<String>> attr : getAttributes().entrySet()) {
			b.append(" ").append(attr.getKey());
			String attrV = attr.getValue().get();
			if(attrV != null) b.append("=\"").append(StringEscapeUtils.escapeHtml4(attrV)).append("\"");
		}
		if(isNoClosingTag()) {
			b.append(">");
			return b.toString(); // Elements without closing tag can't have content
		}
		b.append(">");
		String txt;
		if(getText() != null && (txt = getText().get()) != null) b.append(flags.contains(HtmlElementFlag.DONT_ESCAPE_TEXT) ? txt : StringEscapeUtils.escapeHtml4(txt).replace("\n", "<br/>"));
		for(HtmlElement child : getChildren()) {
			b.append(child.toString());
		}
		b.append("</").append(getType()).append(">");
		return b.toString();
	}

	public static HtmlBr br() {
		return new HtmlBr();
	}

	public static HtmlButton button() {
		return new HtmlButton();
	}

	public static HtmlImg img(Supplier<String> src, Supplier<String> alt) {
		HtmlImg img = new HtmlImg();
		if(src != null) img.setSrc(src);
		if(alt != null) img.setAlt(alt);
		return img;
	}

	public static HtmlImg img(String src, String alt) {
		return img(() -> src, () -> alt);
	}

	public static HtmlLink link() {
		return new HtmlLink();
	}

	public static HtmlMeta meta() {
		return new HtmlMeta();
	}

	public static HtmlOption option() {
		return new HtmlOption();
	}

	public static HtmlScript script() {
		return new HtmlScript();
	}

	public static HtmlSelect select() {
		return new HtmlSelect();
	}

	public static HtmlStyle style() {
		return new HtmlStyle();
	}

	public static HtmlStyle style(StyleSheet style) {
		HtmlStyle st = style();
		st.setText(() -> style.toString());
		return st;
	}

	public static HtmlScript script(JSScript script) {
		HtmlScript sc = script();
		sc.setText(() -> script.toString());
		return sc;
	}

	public static HtmlElement p() {
		return of("p");
	}

	public static HtmlElement of(String type) {
		HtmlElement element = new HtmlElement(type);
		element.setNoClosingTag(VOID_ELEMENTS.contains(type.toLowerCase()));
		return element;
	}

}
