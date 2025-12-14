resource "aws_iam_role" "eks_cluster_role" {
  count = var.enable_eks ? 1 : 0

  name = "${var.project_name}-eks-cluster-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = { Service = "eks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role" "eks_node_role" {
  count = var.enable_eks ? 1 : 0

  name = "${var.project_name}-eks-node-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_eks_cluster" "main" {
  count = var.enable_eks ? 1 : 0

  name     = "${var.project_name}-cluster"
  role_arn = aws_iam_role.eks_cluster_role[0].arn

  vpc_config {
    subnet_ids = [
      aws_subnet.public.id,
      aws_subnet.private_app.id
    ]
    endpoint_private_access = true
    endpoint_public_access  = true
  }

  depends_on = [
    aws_iam_role.eks_cluster_role
  ]
}

resource "aws_eks_node_group" "main" {
  count = var.enable_eks ? 1 : 0

  cluster_name    = aws_eks_cluster.main[0].name
  node_group_name = "general-workers"
  node_role_arn   = aws_iam_role.eks_node_role[0].arn
  subnet_ids      = [aws_subnet.private_app.id]

  scaling_config {
    desired_size = 2
    max_size     = 3
    min_size     = 1
  }

  instance_types = ["t3.medium"]
}