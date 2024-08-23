package me.mrletsplay.simplehttpserver.util;

import java.util.concurrent.LinkedBlockingQueue;

public class RequestResponseQueue<I, O> {

	private LinkedBlockingQueue<Entry<I, O>> entries;

	public RequestResponseQueue() {
	}

	public Entry<I, O> offer(I request, O response) {
		Entry<I, O> entry = new Entry<>(request, response);
		entries.offer(entry);
		return entry;
	}

	public Entry<I, O> poll() {
		return entries.poll();
	}

	public static class Entry<I, O> {

		private I request;
		private O response;

		public Entry(I request, O response) {
			this.request = request;
			this.response = response;
		}

		public I getRequest() {
			return request;
		}

		public void setResponse(O response) {
			this.response = response;
		}

		public O getResponse() {
			return response;
		}

	}

}
