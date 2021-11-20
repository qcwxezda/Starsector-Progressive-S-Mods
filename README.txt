This is a Starsector mod that changes the way built-in hull mods work:

- You can no longer build in hull mods using story points.
- Each ship has its own XP counter and gains XP by dealing damage to opponents during battle.
    - Civilian ships gain a set percentage of all XP gained at the end of a battle.
    - XP gained is proportional to damage dealt and the damaged ship's DP cost.
    - (configurable: XP gain multiplier, civilian ship XP gain percentage)
- Spend a ship's XP to build in hull mods, up to the usual limit.
    - The XP cost of a hull mod depends on the ship's class and DP cost,
        as well as the hull mod's OP cost.  
    - (configurable: base XP cost as a polynomial function of the hull mod's OP cost)
- Use story points to increase the number of hull mods a ship can build in.
    - Cost depends on ship's class and increases each time this option is used.
    - (configurable: change the SP cost scaling or disable this feature entirely)