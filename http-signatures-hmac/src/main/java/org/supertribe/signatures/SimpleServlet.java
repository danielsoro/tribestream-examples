/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package org.supertribe.signatures;

import org.tomitribe.util.IO;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@WebServlet("/touch")
public class SimpleServlet extends HttpServlet {
    private final AtomicInteger count = new AtomicInteger();

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final String out = new StringBuilder(new String(IO.readBytes(req.getInputStream()))).reverse().toString();
        if (count.incrementAndGet() % 2 == 0) {
            resp.getWriter().write(out);
        } else {
            resp.getOutputStream().write(out.getBytes());
        }
    }
}
