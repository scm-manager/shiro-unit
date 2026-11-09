/**
 * The MIT License
 *
 * Copyright (c) 2013, Sebastian Sdorra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */



package com.github.sdorra.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ShiroRuleLifecycleTest
{
  @Test
  public void cleansUpAfterFailedLogin()
  {
    Result result = JUnitCore.runClasses(FailedLogin.class);

    assertEquals(1, result.getFailureCount());
    assertTrue(TrackingSecurityManager.latest.destroyed);
    assertCleanContext();
  }

  @Test
  public void cleansUpAfterTestFailure()
  {
    Result result = JUnitCore.runClasses(FailedTest.class);

    assertEquals(1, result.getFailureCount());
    assertTrue(TrackingSecurityManager.latest.destroyed);
    assertCleanContext();
  }

  @Test
  public void clearsContextEvenWhenDestroyFails()
  {
    TrackingSecurityManager manager = new TrackingSecurityManager()
    {
      @Override
      public void destroy()
      {
        super.destroy();
        throw new AssertionError("destroy failed");
      }
    };
    SecurityUtils.setSecurityManager(manager);
    ThreadContext.bind(manager);
    ThreadContext.bind(new Subject.Builder(manager).buildSubject());

    ShiroRule.tearDownShiro();
    assertTrue(manager.destroyed);
    assertCleanContext();
  }

  @Test
  public void initializesAndCleansUpOnTheEvaluationThread() throws Throwable
  {
    TrackingSecurityManager previous = new TrackingSecurityManager();
    SecurityUtils.setSecurityManager(previous);
    ThreadContext.bind(previous);
    FrameworkMethod method = new FrameworkMethod(FailedTest.class.getMethod("test"));
    Statement statement = new ShiroRule().apply(new Statement()
    {
      @Override
      public void evaluate()
      {
        assertTrue(previous.destroyed);
        assertEquals("trillian", SecurityUtils.getSubject().getPrincipal());
      }
    }, method, new FailedTest());

    assertSame(previous, SecurityUtils.getSecurityManager());
    assertFalse(previous.destroyed);
    statement.evaluate();
    assertTrue(TrackingSecurityManager.latest.destroyed);
    assertCleanContext();
  }

  private void assertCleanContext()
  {
    assertNull(ThreadContext.getSubject());
    assertNull(ThreadContext.getSecurityManager());
    try
    {
      SecurityUtils.getSecurityManager();
      fail("Expected no global security manager");
    }
    catch (UnavailableSecurityManagerException expected)
    {
      // Both the thread-local and global fixtures have been removed.
    }
  }

  public static class TrackingSecurityManager extends DefaultSecurityManager
  {
    static TrackingSecurityManager latest;
    boolean destroyed;

    public TrackingSecurityManager()
    {
      latest = this;
    }

    @Override
    public void destroy()
    {
      destroyed = true;
      super.destroy();
    }
  }

  public static class FailedLogin
  {
    @Rule
    public ShiroRule rule = new ShiroRule();

    @Test
    @SubjectAware(username = "trillian", password = "wrong",
      configuration = "classpath:com/github/sdorra/shiro/lifecycle.ini")
    public void test()
    {
      fail("Failed authentication must prevent the test body from running");
    }
  }

  public static class FailedTest
  {
    @Rule
    public ShiroRule rule = new ShiroRule();

    @Test
    @SubjectAware(username = "trillian", password = "secret",
      configuration = "classpath:com/github/sdorra/shiro/lifecycle.ini")
    public void test()
    {
      throw new AssertionError("test failed");
    }
  }
}
