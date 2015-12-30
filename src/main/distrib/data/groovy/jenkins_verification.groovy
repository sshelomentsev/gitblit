import com.gitblit.GitBlit
import com.gitblit.Keys
import com.gitblit.models.RepositoryModel
import com.gitblit.models.UserModel
import com.gitblit.utils.JGitUtils
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.transport.ReceiveCommand
import org.eclipse.jgit.transport.ReceiveCommand.Result
import org.slf4j.Logger
import java.net.URLEncoder

logger.info("jenkins verification hook triggered by ${user.username} for ${repository.name}")

if (repository.enableCI) {
	def ref = ""
	for (ReceiveCommand command : commands) {
		ref = command.refName
	}
	def triggerUrl = repository.CIUrl + "/gitblit/notifyCommit?"
	def params = "repository=${repository.name}&job=${repository.jobname}&branch=" + ref
	//def enc = URLEncoder.encode(params)
	def url = triggerUrl + params
	logger.info(url)
	new URL(url)
	//new URL(url).getContent() 
}
