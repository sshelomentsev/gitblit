import com.gitblit.utils.RefNameUtils
import com.gitblit.utils.StringUtils
import org.eclipse.jgit.transport.ReceiveCommand
import com.gitblit.models.TicketModel

logger.info("jenkins verification hook triggered by ${user.username} for ${repository.name}")

if (repository.enableCI) {
	def triggerUrl = repository.CIUrl + "/gitblit/notifyCommit?"

	// there's only one ReceiveCommand for branch; even if there are many commits in branch
	def refNames = new HashSet<String>()
	for (ReceiveCommand command : commands) {
		refNames.add(command.refName)
	}

	for (refName in refNames) {
		def requestParams = [
				repository: repository.name,
				job: repository.jobname,
				branch: refName
		]

		def ticketId = RefNameUtils.getTicketId(refName)
		TicketModel ticket = gitblit.ticketService.getTicket(repository, ticketId)

		if (ticket) {
			requestParams << [ticketTitle: "${ticket.title}"]
			requestParams << [ticketTopic: "${ticket.topic}"]
			requestParams << [ticketType: "${ticket.type}"]
			requestParams << [ticketPriority: "${ticket.priority}"]
			requestParams << [user: "${ticket.updatedBy}"]
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