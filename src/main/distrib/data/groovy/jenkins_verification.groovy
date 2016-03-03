import com.gitblit.utils.JGitUtils
import com.gitblit.utils.RefNameUtils
import com.gitblit.utils.StringUtils
import org.apache.commons.io.IOUtils
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.AuthCache
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.eclipse.jgit.transport.ReceiveCommand
import com.gitblit.models.TicketModel

logger.info("jenkins verification hook triggered by ${user.username} for ${repository.name}")

if (repository.enableCI) {
	// there's only one ReceiveCommand for branch; even if there are many commits in branch
	def refNamesWithLastCommits = new HashMap<String, String>()
	for (ReceiveCommand command : commands) {
		refNamesWithLastCommits.put(command.refName,
				JGitUtils.getCommit(receivePack.getRepository(), command.getNewId().getName()).name)
	}

	// schedule Jenkins build for each ref name
	for (refNameWithLastCommit in refNamesWithLastCommits) {
		String requestParamsStr = defineRequestParams(refNameWithLastCommit.key, refNameWithLastCommit.value)
		String uriStr = repository.CIUrl + '/gitblit/notifyCommit?' + requestParamsStr
		logger.info(uriStr)
		URI uri = URI.create(uriStr)
		try {
			sendRequest(uri)
		} catch (IOException e) {
			logger.warn("I/O exception while processing request to URI: ${uri}", e)
		}
	}
}

String defineRequestParams(String refName, String lastCommitSha1) {
	def requestParams = [
			repository: repository.name,
			job: repository.jobname,
			branch: refName,
			user : user,
			lastCommit: lastCommitSha1
	]

	def ticketId = RefNameUtils.getTicketId(refName)
	TicketModel ticket = gitblit.ticketService.getTicket(repository, ticketId)
	if (ticket) {
		requestParams << [ticketTitle: "${ticket.title}"]
		requestParams << [ticketTopic: "${ticket.topic}"]
		requestParams << [ticketType: "${ticket.type}"]
		requestParams << [ticketPriority: "${ticket.priority}"]
	}

	def requestParamsSb = new StringBuilder()
	requestParams.entrySet().each {
		requestParam -> requestParamsSb.append(requestParam.key)
				.append('=')
				.append(StringUtils.encodeURL(requestParam.value.toString()))
				.append('&')
	}
	return requestParamsSb.toString()[0..-2] // remove last '&'
}

private int sendRequest(URI uri) {
	HttpHost host = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme())
	HttpClientContext localContext = null
	HttpClientBuilder clientBuilder = HttpClients.custom()

	// basic authentication
	if (repository.jenkinsUsername?.trim()) {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider()
		credentialsProvider.setCredentials(new AuthScope(uri.getHost(), uri.getPort()),
				new UsernamePasswordCredentials(repository.jenkinsUsername, repository.jenkinsApiToken))
		clientBuilder.setDefaultCredentialsProvider(credentialsProvider)

		AuthCache authCache = new BasicAuthCache()
		BasicScheme basicAuth = new BasicScheme()
		authCache.put(host, basicAuth)
		localContext = HttpClientContext.create()
		localContext.setAuthCache(authCache)
	}

	CloseableHttpClient client = clientBuilder.build()
	CloseableHttpResponse response = null
	try {
		HttpGet request = new HttpGet(uri)
		response = client.execute(host, request, localContext)
		int httpCode = response.statusLine.statusCode
		logger.info("Response HTTP status code: ${httpCode}")
		return httpCode
	} finally {
		EntityUtils.consumeQuietly(response.getEntity())
		IOUtils.closeQuietly(client)
	}
}