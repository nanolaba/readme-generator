<!--@nrg.languages=en,ru,fr-->
<!--@globalParameter=This parameter is visible in all imported files-->
<!--testComment1-->
${widget:import(path='test.txt')}
${widget:import(path='folder/test.txt')}
${widget:import(path='folder/testWin1251.txt', charset='windows-1251')}
${widget:import(path='folder/test.src.md')}
${widget:import(path='folder/test-no-generation.src.md', run-generator='false')}
