/**
 * Copyright (C) 2014 Premium Minds.
 *
 * This file is part of pm-wicket-utils.
 *
 * pm-wicket-utils is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * pm-wicket-utils is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with pm-wicket-utils. If not, see <http://www.gnu.org/licenses/>.
 */
package com.premiumminds.webapp.wicket.testing;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestHandler;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxRequestTarget.IListener;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.ILogData;
import org.apache.wicket.request.IRequestCycle;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.IContextProvider;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Base class for creating unit tests for wicket components. The core of the class is based on
 * {@link WicketTester#startComponentInPage(Component)}, and it uses {@link EasyMockSupport#createMockBuilder(Class)}
 * to create a mock of {@link AjaxRequestTarget} with which to verify Ajax calls from the component.
 * Note that {@link AjaxRequestTarget#addListener(IListener)}, {@link AjaxRequestTarget#respond(IRequestCycle)}
 * and {@link AjaxRequestTarget#detach(IRequestCycle)} are ignored by the mock, as they are used by the wicket
 * framework itself.
 */
public abstract class AbstractComponentTest extends EasyMockSupport implements IContextProvider<AjaxRequestTarget, Page> {
	private class RequestTargetTester implements AjaxRequestTarget {
		private AjaxRequestTarget inner;

		@SuppressWarnings("unused")
		public RequestTargetTester(Page page) {
			inner = new AjaxRequestHandler(page);
		}

		@Override
		public Integer getPageId() {
			return inner.getPageId();
		}

		@Override
		public boolean isPageInstanceCreated() {
			return inner.isPageInstanceCreated();
		}

		@Override
		public Integer getRenderCount() {
			return inner.getRenderCount();
		}

		@Override
		public Class<? extends IRequestablePage> getPageClass() {
			return inner.getPageClass();
		}

		@Override
		public PageParameters getPageParameters() {
			return inner.getPageParameters();
		}

		@Override
		public void respond(IRequestCycle requestCycle) {
			inner.respond(requestCycle);
		}

		@Override
		public void detach(IRequestCycle requestCycle) {
			inner.detach(requestCycle);
		}

		@Override
		public ILogData getLogData() {
			return inner.getLogData();
		}

		@Override
		public void add(Component component, String markupId) {
			inner.add(component, markupId);
		}

		@Override
		public void add(Component... components) {
			inner.add(components);
		}

		@Override
		public void addChildren(MarkupContainer parent, Class<?> childCriteria) {
			inner.addChildren(parent, childCriteria);
		}

		@Override
		public void addListener(IListener listener) {
			inner.addListener(listener);
		}

		@Override
		public void appendJavaScript(CharSequence javascript) {
			inner.appendJavaScript(javascript);
		}

		@Override
		public void prependJavaScript(CharSequence javascript) {
			inner.prependJavaScript(javascript);
		}

		@Override
		public void registerRespondListener(ITargetRespondListener listener) {
			inner.registerRespondListener(listener);
		}

		@Override
		public Collection<? extends Component> getComponents() {
			return inner.getComponents();
		}

		@Override
		public void focusComponent(Component component) {
			inner.focusComponent(component);
		}

		@Override
		public IHeaderResponse getHeaderResponse() {
			return inner.getHeaderResponse();
		}

		@Override
		public String getLastFocusedElementId() {
			return inner.getLastFocusedElementId();
		}

		@Override
		public Page getPage() {
			return inner.getPage();
		}
	}

	private static final Method[] methods = initMethods(); 

	private WebApplication wicketApp;
	private ExtendedWicketTester tester;
	private IContextProvider<AjaxRequestTarget, Page> orig;
	private AjaxRequestTarget target;
	private boolean running;

	public AbstractComponentTest() {
		wicketApp = new WebApplication() {
			@Override
			public Class<? extends Page> getHomePage() {
				return null;
			}
		};
	}

	/**
	 * Rule for expecting thrown exceptions during test execution. Preferred over <tt>expected=</tt> in a <tt>@Test</tt>
	 * annotation.
	 * The test should call {@link ExpectedException#expect(Class)} or a similar method to specify that it expects an
	 * exception to be thrown subsequently.
	 */
	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * Code that runs before every test. If this method is overriden, the override <b>must</b> call <tt>super.setUp()</tt>.
	 * The preferred alternative is to simply create a differently named method and annotate it with <tt>@Before</tt>.
	 */
	@Before
	public void setUp() {
		tester = new ExtendedWicketTester(wicketApp);
		running = false;
	}

	/**
	 * Code that runs after every test. If this method is overriden, the override <b>must</b> call <tt>super.tearDown()</tt>.
	 * The preferred alternative is to simply create a differently named method and annotate it with <tt>@After</tt>.
	 */
	@After
	public void tearDown() {
		if (running) {
			running = false;
			wicketApp.setAjaxRequestTargetProvider(orig);
		}
	}

	/**
	 * Starts a component for testing in a bare-bones page generated by the wicket framework. Creates and injects
	 * a mock AJAX request target for the component to interact with.
	 * See {@link WicketTester#startComponentInPage(Component)}.
	 * Note that {@link AjaxRequestTarget#addListener(IListener)}, {@link AjaxRequestTarget#respond(IRequestCycle)}
	 * and {@link AjaxRequestTarget#detach(IRequestCycle)} are ignored by the mock, as they are used by the wicket
	 * framework itself.
	 * 
	 * @param subject
	 * 				The component to be tested
	 */
	protected void startTest(Component subject) {
		startTest(subject, true);
	}

	/**
	 * Starts a component for testing in a bare-bones form in a simple page. Creates and injects a mock AJAX
	 * request target for the component to interact with.
	 * See {@link WicketTester#startComponentInPage(Component)}.
	 * Note that {@link AjaxRequestTarget#addListener(IListener)}, {@link AjaxRequestTarget#respond(IRequestCycle)}
	 * and {@link AjaxRequestTarget#detach(IRequestCycle)} are ignored by the mock, as they are used by the wicket
	 * framework itself.
	 * 
	 * @param subject
	 * 				The component to be tested
	 * @param inputType
	 * 				The HTML type of the input to associate with the component to be tested
	 */
	protected void startFormComponentTest(Component subject, String inputType) {
		startFormComponentTest(subject, inputType, true);
	}

	/**
	 * Starts a component for testing in a bare-bones page generated by the wicket framework. Optionally creates
	 * and injects a mock AJAX request target for the component to interact with.
	 * See {@link WicketTester#startComponentInPage(Component)}.
	 * Note that {@link AjaxRequestTarget#addListener(IListener)}, {@link AjaxRequestTarget#respond(IRequestCycle)}
	 * and {@link AjaxRequestTarget#detach(IRequestCycle)} are ignored by the mock, as they are used by the wicket
	 * framework itself.
	 * 
	 * @param subject
	 * 				The component to be tested
	 * @param mockRequest
	 * 				If the parameter is true, a mock request is created. Use this to verify the component's calls into Ajax.
	 * 				If the parameter is false, a mock request is not created and the default request created by the wicket
	 *              framework is used instead. Use this to call into the component from Ajax.
	 */
	protected void startTest(Component subject, boolean mockRequest) {
		if (!running) {
			tester.startComponentInPage(subject);
			innerStartTest(mockRequest);
		}
	}

	/**
	 * Starts a component for testing in a bare-bones form in a simple page. Optionally creates and injects a mock AJAX
	 * request target for the component to interact with.
	 * See {@link WicketTester#startComponentInPage(Component)}.
	 * Note that {@link AjaxRequestTarget#addListener(IListener)}, {@link AjaxRequestTarget#respond(IRequestCycle)}
	 * and {@link AjaxRequestTarget#detach(IRequestCycle)} are ignored by the mock, as they are used by the wicket
	 * framework itself.
	 * 
	 * @param subject
	 * 				The component to be tested
	 * @param inputType
	 * 				The HTML type of the input to associate with the component to be tested
	 * @param mockRequest
	 * 				If the parameter is true, a mock request is created. Use this to verify the component's calls into Ajax.
	 * 				If the parameter is false, a mock request is not created and the default request created by the wicket
	 *              framework is used instead. Use this to call into the component from Ajax.
	 */
	protected void startFormComponentTest(Component subject, String inputType, boolean mockRequest) {
		if (!running) {
			tester.startComponentInForm(subject, inputType);
			innerStartTest(mockRequest);
		}
	}

	/**
	 * @return A reference to the {@link WicketTester} object created during test set-up.
	 */
	protected ExtendedWicketTester getTester() {
		return tester;
	}

	/**
	 * @return A reference to the mock Ajax request target object created by {@link #startTest(Component)}
	 * or {@link #startTest(Component, boolean)}. The returned object is useless if <tt>startTest</tt> is called with
	 * <tt>mockRequest</tt>=<tt>false</tt>, but it is guaranteed not to be <tt>null</tt>.
	 * @throws TestNotStartedException if this method is called before those methods.
	 */
	protected AjaxRequestTarget getTarget() throws TestNotStartedException {
		if (!running)
			throw new TestNotStartedException();

		return target;
	}

	/**
	 * This method is a part of <tt>IContextProvider&lt;AjaxRequestTarget, Page&gt;</tt> implementation and is not
	 * meant to be called by user code.
	 * 
	 * @see org.apache.wicket.util.IContextProvider#get(java.lang.Object)
	 */
	@Override
	public AjaxRequestTarget get(Page context) {
		return target;
	}

	private void innerStartTest(boolean mockRequest) {
		target = createMockBuilder(RequestTargetTester.class)
				.addMockedMethods(methods)
				.withConstructor(this, tester.getLastRenderedPage())
				.createStrictMock();
		orig = wicketApp.getAjaxRequestTargetProvider();
		if (mockRequest)
			wicketApp.setAjaxRequestTargetProvider(this);
		running = true;
	}

	//This method may need to be expanded if we find further unexpected calls into the request target
	//from inside the Wicket framework - JMMM
	private static Method[] initMethods() {
		HashSet<Method> aux = new HashSet<Method>(Arrays.asList(RequestTargetTester.class.getDeclaredMethods()));
		aux.remove(getMethodHelper("addListener", AjaxRequestTarget.IListener.class));
		aux.remove(getMethodHelper("respond", IRequestCycle.class));
		aux.remove(getMethodHelper("detach", IRequestCycle.class));
		return aux.toArray(new Method[aux.size()]);
	}

	private static Method getMethodHelper(String name, Class<?>... parameterTypes) {
		try {
			return RequestTargetTester.class.getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}
}
