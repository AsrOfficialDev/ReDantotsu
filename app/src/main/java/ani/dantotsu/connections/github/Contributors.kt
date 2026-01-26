package ani.dantotsu.connections.github

import ani.dantotsu.Mapper
import ani.dantotsu.R
import ani.dantotsu.client
import ani.dantotsu.getAppString
import ani.dantotsu.settings.Developer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

class Contributors {

    data class DeveloperSections(
        val redantotsuDevs: Array<Developer>,
        val dantotsuDevs: Array<Developer>
    )

    suspend fun getContributorSections(): DeveloperSections {
        // ReDantotsu developers (only AsrOfficialDev)
        val redantotsuDevs = arrayOf(
            Developer(
                "AsrOfficialDev",
                "https://github.com/AsrOfficialDev.png", 
                "ReDantotsu Developer",
                "https://github.com/AsrOfficialDev"
            )
        )
        
        // Dantotsu developers - start with hardcoded list
        var dantotsuDevs = arrayOf(
            Developer(
                "MarshMeadow",
                "https://avatars.githubusercontent.com/u/88599122?v=4",
                "Beta Icon Designer & Website Maintainer",
                "https://github.com/MarshMeadow?tab=repositories"
            ),
            Developer(
                "Zaxx69",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6342562-kxE8m4i7KUMK.png",
                "Telegram Admin",
                "https://anilist.co/user/6342562"
            ),
            Developer(
                "Arif Alam",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6011177-2n994qtayiR9.jpg",
                "Discord & Comment Moderator",
                "https://anilist.co/user/6011177"
            ),
            Developer(
                "SunglassJeery",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b5804776-FEKfP5wbz2xv.png",
                "Head Discord & Comment Moderator",
                "https://anilist.co/user/5804776"
            ),
            Developer(
                "Excited",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6131921-toSoGWmKbRA1.png",
                "Comment Moderator",
                "https://anilist.co/user/6131921"
            ),
            Developer(
                "Gurjshan",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6363228-rWQ3Pl3WuxzL.png",
                "Comment Moderator",
                "https://anilist.co/user/6363228"
            ),
            Developer(
                "NekoMimi",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6244220-HOpImMGMQAxW.jpg",
                "Comment Moderator",
                "https://anilist.co/user/6244220"
            ),
            Developer(
                "Ziadsenior",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6049773-8cjYeUOFUguv.jpg",
                "Comment Moderator and Arabic Translator",
                "https://anilist.co/user/6049773"
            ),
            Developer(
                "Dawnusedyeet",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6237399-RHFvRHriXjwS.png",
                "Contributor",
                "https://anilist.co/user/Dawnusedyeet/"
            ),
            Developer(
                "hastsu",
                "https://s4.anilist.co/file/anilistcdn/user/avatar/large/b6183359-9os7zUhYdF64.jpg",
                "Comment Moderator and Arabic Translator",
                "https://anilist.co/user/6183359"
            ),
        )
        
        // Try to fetch from GitHub API
        try {
            val repo = getAppString(R.string.repo)
            val res = client.get("https://api.github.com/repos/$repo/contributors")
                .parsed<JsonArray>().map {
                    Mapper.json.decodeFromJsonElement<GithubResponse>(it)
                }
            
            // Add GitHub contributors that aren't already in the list
            val existingNames = dantotsuDevs.map { it.name.lowercase() }.toSet() + setOf("asrofficialdev")
            
            res.forEach {
                if (it.login.lowercase() in existingNames || it.login == "SunglassJerry") return@forEach
                val role = when (it.login) {
                    "rebelonion" -> "Owner & Maintainer"
                    "sneazy-ibo" -> "Contributor & Comment Moderator"
                    "WaiWhat" -> "Icon Designer"
                    "itsmechinmoy" -> "Discord and Telegram Admin/Helper, Comment Moderator & Translator"
                    else -> "Contributor"
                }
                dantotsuDevs = arrayOf(
                    Developer(
                        it.login,
                        it.avatarUrl,
                        role,
                        it.htmlUrl
                    )
                ) + dantotsuDevs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return DeveloperSections(redantotsuDevs, dantotsuDevs)
    }

    // Keep old method for backward compatibility
    suspend fun getContributors(): Array<Developer> {
        val sections = getContributorSections()
        return sections.redantotsuDevs + sections.dantotsuDevs
    }


    @Serializable
    data class GithubResponse(
        @SerialName("login")
        val login: String,
        @SerialName("avatar_url")
        val avatarUrl: String,
        @SerialName("html_url")
        val htmlUrl: String
    )
}
