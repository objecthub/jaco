let rec fib n = if (n = 0) then
				1
			else if (n = 1) then
				1
			else
				(fib (n - 1)) + (fib (n - 2))
in
fib 35;;
