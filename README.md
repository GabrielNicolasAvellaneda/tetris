#tetris

This was a class project for a course on Reinforcement Learning. The documentation can be found at: https://github.com/shubhomoydas/tetris/blob/master/documentation/class-project-report.pdf

To compile:
-------------
    javac -d ./bin $(find ./src/*/* | grep .java)
    jar cvf ./tetris.jar -C ./bin/ .

To run Tetris:
-------------
java -cp ./tetris.jar com.smd.tetris.TetrisApp

When started, it will first display an empty grid for the initial training epochs. Once trained, it will start playing on its own and render the play on the grid.
