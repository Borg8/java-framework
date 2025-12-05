package borg.framework.services;

import com.google.auth.oauth2.GoogleCredentials;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;

import borg.framework.structures.HttpResponse;

public class FcmSender
{
	/*************************************************************************************************
	 * Constants
	 ************************************************************************************************/

	private static final String URL_FCM_TEMPLATE = "https://fcm.googleapis.com/v1/projects/%s/messages:send";

	private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

	/*************************************************************************************************
	 * Fields
	 ************************************************************************************************/

	/** HTTP client instance **/
	private final HttpClient mHttpClient;

	/** path to service account JSON file **/
	private String mServiceAccountPath;

	/** Firebase project ID **/
	private String mProjectId;

	/** cached Google credentials **/
	private GoogleCredentials mCredentials;

	/** single instance of FcmSender **/
	private static FcmSender sInstance = null;

	/*************************************************************************************************
	 * Methods
	 ************************************************************************************************/

	public FcmSender()
	{
		mHttpClient = new HttpClient();
		mServiceAccountPath = null;
		mProjectId = null;
		mCredentials = null;
	}

	/**
	 * @return single instance of FcmSender.
	 */
	@NotNull
	public static FcmSender getInstance()
	{
		if (sInstance == null)
		{
			// create single instance of FcmSender
			sInstance = new FcmSender();
		}

		return sInstance;
	}

	/**
	 * configure service account path and project ID.
	 */
	public void setConfiguration(@NotNull String serviceAccountPath_, @NotNull String projectId_)
	{
		mServiceAccountPath = serviceAccountPath_;
		mProjectId = projectId_;
		mCredentials = null;
	}

	/**
	 * send data message to device. Blocking operation.
	 *
	 * @param token_ device token.
	 * @param data_ message to send (values must be Strings).
	 * @return FCM response.
	 */
	@NotNull
	public HttpResponse sendMessage(@NotNull String token_, @NotNull JSONObject data_)
	{
		if (mProjectId == null || mServiceAccountPath == null)
		{
			throw new Error("FcmSender not configured. Call setConfiguration() first");
		}

		// Get valid OAuth 2.0 token
		String accessToken = getAccessToken();

		// prepare payload (HTTP v1 structure)
		JSONObject message = new JSONObject();
		message.put("token", token_);
		message.put("data", data_);

		// Android specific configuration for priority
		JSONObject androidConfig = new JSONObject();
		androidConfig.put("priority", "HIGH");
		message.put("android", androidConfig);

		JSONObject root = new JSONObject();
		root.put("message", message);

		// prepare headers
		Map<String, String> headers = Map.of(
			"Content-Type", "application/json",
			"Authorization", "Bearer " + accessToken);

		// send request
		String url = String.format(URL_FCM_TEMPLATE, mProjectId);
		byte[] content = root.toString().getBytes();

		return mHttpClient.sendRequest(url, "POST", headers, content);
	}

	/**
	 * retrieve and refresh OAuth 2.0 token.
	 */
	private String getAccessToken()
	{
		try
		{
			if (mCredentials == null)
			{
				try (FileInputStream stream = new FileInputStream(mServiceAccountPath))
				{
					mCredentials = GoogleCredentials.fromStream(stream)
						.createScoped(Collections.singletonList(SCOPE));
				}
			}

			mCredentials.refreshIfExpired();
			return mCredentials.getAccessToken().getTokenValue();
		}
		catch (Throwable e_)
		{
			throw new Error(e_);
		}
	}
}