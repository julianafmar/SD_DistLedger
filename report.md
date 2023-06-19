Relatório: Grupo 33

Para esta terceira entrega, decidimos optar por implementar um projeto que suporta 3 servidores, para  os conseguir organizar melhor alterámos o proto CrossNamingServer, para que o comando register devolvesse um inteiro único a cada servidor que o identifica.

Relativamente ao gossip, este comando é feito de um servidor para os outro todos, pelo que este comando só recebe um qualifier.

Para conseguir que todas as réplicas tenham a mesma ordem do ledgerstate, decidimos mudar a estrutura da ledger para um Map cujas keys são os ids dos servidores e cujo value é uma lista de todas as operações que foram pedidas ao servidor correspondente ao id.

Para melhorar a comunicação entre as réplicas, quando uma operação é cancelada, ou seja, quando apesar de já ser considera estável, esta não cumpre com os requisitos da mesma, a sua valueTS fica com um -1 no índice de id igual ao servidor que a cancelou, informando as outras réplicas de que esta operação não deve mudar o estado e quando determinada operação é instável coloca no valueTS -2.

Tal como fizemos na segunda entrega, o propagate state envia a ledger inteira, pois considerámos o caso em que um servidor poderia ser criado mais tarde e sendo assim necessitaria de toda a ledger para que o seu estado fique igual ao dos outros, contudo quando um servidor recebe uma ledger por gossip, este só irá buscar as operações que não tem.