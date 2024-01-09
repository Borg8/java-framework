package borg.framework.services;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import borg.framework.auxiliaries.Logger;

public class TcpServer
{
	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Public Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Constants
	//////////////////////////////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Definitions
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@FunctionalInterface
	public interface Listener
	{
		/**
		 * connection accepted.
		 *
		 * @param socket_ connected socket, or {@code null} if no socket connected.
		 */
		void onAccept(@Nullable Socket socket_);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Fields
	//////////////////////////////////////////////////////////////////////////////////////////////////

	/** server socket **/
	private ServerSocket mServer;

	/** listened port **/
	private int mPort;

	//////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods
	//////////////////////////////////////////////////////////////////////////////////////////////////

	@Contract(pure = true)
	public TcpServer()
	{
		mPort = -1;
	}

	/**
	 * @return listened port, or -1 if no port listened.
	 */
	public int getPort()
	{
		return mPort;
	}

	/**
	 * accept connections.
	 *
	 * @param port_     port to accept to.
	 * @param listener_ connections listener.
	 *
	 * @return {@code true} if the server started, {@code false} if the server already started.
	 */
	public synchronized boolean accept(int port_, @NotNull Listener listener_)
	{
		// if not busy
		if (mPort < 0)
		{
			// create server socket
			try
			{
				mPort = port_;
				mServer = new ServerSocket(port_);
			}
			catch (Exception e)
			{
				throw new Error(e);
			}

			// accept connections
			TasksManager.runOnThread(param1_ ->
			{
				Thread.currentThread().setName("HTTP server on " + mPort);

				try
				{
					// accept connections
					//noinspection InfiniteLoopStatement
					while (true)
					{
						// accept connections
						Socket socket = mServer.accept();

						// invoke listener
						TasksManager.runOnThread(param2_ ->
						{
							Thread.currentThread().setName("Accepted socket: " + socket.getInetAddress());

							try
							{
								listener_.onAccept(socket);
							}
							catch (Throwable e1)
							{
								Logger.log(e1);
								try
								{
									// write error
									socket.close();
								}
								catch (IOException e2)
								{
									Logger.log(e2);
								}
							}
						});
					}
				}
				catch (IOException e)
				{
					Logger.log(e);
				}

				// invoke listeners
				listener_.onAccept(null);
			});

			return true;
		}

		return false;
	}

	/**
	 * stop server.
	 */
	public synchronized void stop()
	{
		if (mPort > 0)
		{
			// close server socket
			mPort = -1;
			try
			{
				mServer.close();
			}
			catch (Exception e)
			{
				Logger.log(e);
			}
		}
	}
}
