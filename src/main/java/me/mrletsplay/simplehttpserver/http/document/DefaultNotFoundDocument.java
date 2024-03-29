package me.mrletsplay.simplehttpserver.http.document;

import me.mrletsplay.simplehttpserver.dom.html.HtmlDocument;
import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;
import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.header.HttpServerHeader;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;

public class DefaultNotFoundDocument implements HttpDocument {

	private HtmlDocument doc;

	public DefaultNotFoundDocument() {
		this.doc = new HtmlDocument();
		doc.setTitle("404 Not Found");
		doc.setDescription("404 Page");

		HtmlElement h1 = new HtmlElement("h1");
		h1.setText("404 Not Found");
		doc.getBodyNode().appendChild(h1);
		doc.getBodyNode().appendChild(HtmlElement.br());

		HtmlElement i = new HtmlElement("i");
		i.setText(() -> HttpRequestContext.getCurrentContext().getClientHeader().getPath().getDocumentPath() + " was not found on this server");
		doc.getBodyNode().appendChild(i);
		doc.getBodyNode().appendChild(HtmlElement.br());

		HtmlElement p = HtmlElement.p();
		p.setText("SimpleHttpServer (Java)");
		p.setAttribute("style", "font-size: 12px");
		doc.getBodyNode().appendChild(p);
	}

	@Override
	public void createContent() {
		HttpServerHeader h = HttpRequestContext.getCurrentContext().getServerHeader();
		doc.createContent();
		h.setStatusCode(HttpStatusCodes.NOT_FOUND_404);
	}

}
