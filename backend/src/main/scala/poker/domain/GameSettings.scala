package poker.domain

case class GameSettings(
                         name: String = "",           // empty = auto-generated
                         smallBlind: Int = 10,
                         bigBlind: Int = 20,
                         initialChips: Int = 1000,
                         isPublic: Boolean = false,  
                         maxPlayers: Int = 9          
                       )