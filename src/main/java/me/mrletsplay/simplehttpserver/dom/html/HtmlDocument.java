package me.mrletsplay.simplehttpserver.dom.html;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.dom.html.element.HtmlLink;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlMeta;
import me.mrletsplay.simplehttpserver.dom.html.element.HtmlScript;
import me.mrletsplay.simplehttpserver.http.document.HttpDocument;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class HtmlDocument implements HttpDocument {

	private HtmlElement
		html,
		head,
		body,
		title;

	private HtmlMeta
		description;

	private HtmlLink
		icon;

	public HtmlDocument() {
		this.html = new HtmlElement("html");
		this.head = new HtmlElement("head");
		this.body = new HtmlElement("body");
		this.title = new HtmlElement("title");
		title.setText("Webinterface");
		this.description = HtmlElement.meta();
		description.setName("description");
		description.setContent("Just another HTML5 document!");
		this.icon = HtmlElement.link();
		icon.setRel("icon");
		icon.setHref("/favicon.ico");
		HtmlMeta vp = HtmlElement.meta();
		vp.setName("viewport");
		vp.setContent("width=device-width, initial-scale=1");
		head.appendChild(title);
		head.appendChild(description);
		head.appendChild(icon);
		head.appendChild(vp);
		html.appendChild(head);
		html.appendChild(body);
	}

	public HtmlElement getParentNode() {
		return html;
	}

	public HtmlElement getHeadNode() {
		return head;
	}

	public HtmlElement getBodyNode() {
		return body;
	}

	public HtmlElement getTitleNode() {
		return title;
	}

	public HtmlMeta getDescriptionNode() {
		return description;
	}

	public HtmlLink getIconNode() {
		return icon;
	}

	public void addStyleSheet(String path) {
		HtmlLink el = HtmlElement.link();
		el.setRel("stylesheet");
		el.setLinkType("text/css");
		el.setHref(path);
		head.appendChild(el);
	}

	public void includeScript(String src, boolean async) {
		HtmlScript sc = HtmlElement.script();
		sc.setSource(src);
		sc.setAsync(async);
		head.appendChild(sc);
	}

	public void includeScript(String src, boolean async, boolean defer) {
		HtmlScript sc = HtmlElement.script();
		sc.setSource(src);
		sc.setAsync(async);
		sc.setDefer(defer);
		head.appendChild(sc);
	}

	public void setTitle(Supplier<String> title) {
		this.title.setText(title);
	}

	public void setTitle(String title) {
		setTitle(() -> title);
	}

	public void setDescription(Supplier<String> description) {
		this.description.setAttribute("content", description);
	}

	public void setDescription(String description) {
		setDescription(() -> description);
	}

	public void setIcon(Supplier<String> icon) {
		this.icon.setHref(icon);
	}

	public void setIcon(String icon) {
		setIcon(() -> icon);
	}

	public void setLanguage(String lang) {
		html.setAttribute("lang", lang);
	}

	@Override
	public void createContent() {
		HttpRequestContext.getCurrentContext().getServerHeader().setContent(MimeType.HTML, ("<!DOCTYPE html>" + html.toString()).getBytes(StandardCharsets.UTF_8));
	}

}
