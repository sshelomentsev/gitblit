import com.gitblit.utils.JGitUtils
import com.gitblit.utils.RefNameUtils
import com.gitblit.utils.StringUtils
import org.eclipse.jgit.transport.ReceiveCommand
import com.gitblit.models.TicketModel

logger.info("jenkins verification hook triggered by ${user.username} for ${repository.name}")

if (repository.enableCI) {
	def triggerUrl = repository.CIUrl + "/gitblit/notifyCommit?"

	// there's only one ReceiveCommand for branch; even if there are many commits in branch
	def refNamesWithLastCommits = new HashMap<String, String>()
	for (ReceiveCommand command : commands) {
		refNamesWithLastCommits.put(command.refName,
				JGitUtils.getCommit(receivePack.getRepository(), command.getNewId().getName()).name)
	}

	for (refNameWithLastCommit in refNamesWithLastCommits) {
		String refName = refNameWithLastCommit.key
		String lastCommitSha1 = refNameWithLastCommit.value
		def requestParams = [
				repository: repository.name,
				job: repository.jobname,
				branch: refName,
				user : user,
				authToken: repository.jenkinsAuthToken,
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
		String paramsStr = requestParamsSb.toString()[0..-2] // remove last '&'

		String url = triggerUrl + paramsStr
		logger.info(url)

		new URL(url).openConnection().getResponseCode()
	} // next refName
}