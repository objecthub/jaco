do
let fib(n) = if (n = 0) then
				reply 1
			else if (n = 1) then
				reply 1
			else
				reply (fib (n - 1)) + (fib (n - 2))
in
	print(fib(25));;
	
