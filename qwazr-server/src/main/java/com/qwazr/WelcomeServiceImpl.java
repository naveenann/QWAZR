package com.qwazr;

import com.qwazr.utils.server.ServiceInterface;
import com.qwazr.utils.server.ServiceName;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;

@RolesAllowed("welcome")
@Path("/")
@ServiceName("welcome")
public class WelcomeServiceImpl implements ServiceInterface {

	@GET
	@Produces(ServiceInterface.APPLICATION_JSON_UTF8)
	public WelcomeStatus welcome(@QueryParam("properties") Boolean properties, @QueryParam("env") Boolean env) {
		return new WelcomeStatus(properties, env);
	}

	@DELETE
	@Path("/shutdown")
	public void shutdown() {
		new ShutdownThread();
	}

	private static class ShutdownThread implements Runnable {

		private ShutdownThread() {
			new Thread(this).start();
		}

		@Override
		public void run() {
			try {
				Thread.sleep(5000);
				Qwazr.stop(null);
			} catch (InterruptedException e) {
				Qwazr.LOGGER.warn(e.getMessage(), e);
			}
		}
	}
}
