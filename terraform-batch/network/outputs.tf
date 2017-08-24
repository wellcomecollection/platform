output "subnets" {
  value = ["${aws_subnet.network.*.id}"]
}

output "vpc_id" {
  value = "${aws_vpc.vpc.id}"
}
