# Dipblue - A DipGame Bot

DipBlue bot for the Diplomacy Simulator DipGame. 

This project was created during my Masters Thesis with the objective to create a negotiating bot for DipGame with the ability to do Trust Reasoning combined with a simple typical game search based on the board alone. 

Two scientific papers were made after the Master Thesis, documenting the results found. Both are published in the following books:
* [Proceedings of the International Conference on Agents and Artificial Intelligence - (Volume 1), 2015](http://www.scitepress.org/DigitalLibrary/ProceedingsDetails.aspx?ID=OdYcBv99mTw=&t=1)
* [Transactions on Computational Collective Intelligence XX, 2015](http://www.springer.com/gp/book/9783319275420)

## Installation

The installation of this project is similar to [the guide to create a DipGame bot](http://www.dipgame.org/browse/tutorial#CreateBot) provided by Angela Fabregues and the other developers/contributors of the [DipGame](http://www.dipgame.org/).

## Usage

The DipBlue bot can be customized through the `DipBlueBuilder` class. This is done by the GameLauncher class inside my custom [`GameLauncher`](https://github.com/andreferreirav2/dipgame_gamemanager) in which there are multiple versions of the bot, named [`Archetypes`](https://github.com/andreferreirav2/dipgame_gamemanager/blob/master/src/pt/up/fe/mieic/andreferreira/dipblue/game/GameLauncher.java#L181). 
Below are two examples of Archetypes:

```java
/**
 * No communication at all
 */
NO_PRESS(false, false, false, false, false,
        new Class[]{AdviserMapTactician.class},
        new double[]{1.0}),
```

```java
/**
 * With communication, accepts with criteria, requests and keeps trust scores
 */
DIPBLUE(true, true, false, true, true,
        new Class[]{AdviserMapTactician.class, AdviserAgreementExecutor.class, AdviserTeamBuilder.class, AdviserWordKeeper.class},
        new double[]{1.0, 1.0, 1.0, 1.0, 1.0});
```

## Contributing

1. Fork it!
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request :D

## Credits

* [André Ferreira](https://github.com/andreferreirav2)
*  [Henrique Lopes Cardoso (FEUP)](https://up-pt.academia.edu/HenriqueCardoso) and [Luis Paulo Reis (UMinho)](http://uminho.academia.edu/LuisPauloReis)
* [Angela Fabregues and the other developers/contributors of the DipGame](http://www.dipgame.org/).

## License

MIT: http://rem.mit-license.org