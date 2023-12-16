package me.mrletsplay.simplehttpserver.http.document;

import me.mrletsplay.simplehttpserver.dom.html.HtmlDocument;
import me.mrletsplay.simplehttpserver.dom.html.HtmlElement;
import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.header.HttpServerHeader;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;

public class DefaultErrorDocument implements HttpDocument {

	private HtmlDocument doc;

	public DefaultErrorDocument() {
		this.doc = new HtmlDocument();
		doc.setTitle("500 Internal Server Error");
		doc.setDescription("500 Page");

		HtmlElement h1 = new HtmlElement("h1");
		h1.setText("500 Internal Server Error");
		doc.getBodyNode().appendChild(h1);
		doc.getBodyNode().appendChild(HtmlElement.br());

		HtmlElement i = new HtmlElement("i");
		i.setText("An unexpected error occurred while processing your request");
		doc.getBodyNode().appendChild(i);
		doc.getBodyNode().appendChild(HtmlElement.br());

		HtmlElement add = new HtmlElement("pre");
		add.setText(() -> {
			HttpRequestContext context = HttpRequestContext.getCurrentContext();
			if(!context.getServer().getConfiguration().isDebugMode()) return "Enable debug mode to see more information about this error";
			StringBuilder b = new StringBuilder("Stack trace:\n");
			Exception e = HttpRequestContext.getCurrentContext().getException();
			append(b, e, false);
			return b.toString();
		});
		doc.getBodyNode().appendChild(add);

		HtmlElement p = HtmlElement.p();
		p.setText("SimpleHttpServer (Java)");
		p.setAttribute("style", "font-size: 12px");
		doc.getBodyNode().appendChild(p);
	}

	private void append(StringBuilder b, Throwable t, boolean isCause) {
		if(isCause) b.append("\nCaused by: ");
		b.append(t.toString());
		for(StackTraceElement el : t.getStackTrace()) {
			b.append("\n\t").append(el.toString());
		}
		if(t.getCause() != null) append(b, t.getCause(), true);
	}

	@Override
	public void createContent() {
		HttpServerHeader h = HttpRequestContext.getCurrentContext().getServerHeader();
		doc.createContent();
		h.setStatusCode(HttpStatusCodes.INTERNAL_SERVER_ERROR_500);
	}

}
